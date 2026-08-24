package com.tguard.tguard_backend.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.kafka.entity.TransactionOutboxEvent;
import com.tguard.tguard_backend.kafka.producer.TransactionEventProducer;
import com.tguard.tguard_backend.kafka.repository.TransactionOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionOutboxServiceTest {

    private TransactionOutboxEventRepository outboxRepository;
    private TransactionEventProducer transactionEventProducer;
    private TransactionOutboxService transactionOutboxService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(TransactionOutboxEventRepository.class);
        transactionEventProducer = mock(TransactionEventProducer.class);

        KafkaTopicProperties topicProperties = new KafkaTopicProperties();
        topicProperties.setRealtime("transactions.realtime");
        topicProperties.setBatch("transactions.batch");

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        transactionOutboxService = new TransactionOutboxService(
                outboxRepository,
                transactionEventProducer,
                topicProperties,
                objectMapper
        );
    }

    @Test
    void enqueueStoresOneOutboxEventPerTopic() {
        TransactionEvent event = event();

        transactionOutboxService.enqueue(event);

        ArgumentCaptor<TransactionOutboxEvent> captor = ArgumentCaptor.forClass(TransactionOutboxEvent.class);
        verify(outboxRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<TransactionOutboxEvent> saved = captor.getAllValues();
        assertThat(saved).extracting(TransactionOutboxEvent::getTopic)
                .containsExactly("transactions.realtime", "transactions.batch");
        assertThat(saved).allSatisfy(outboxEvent -> {
            assertThat(outboxEvent.getTenantId()).isEqualTo("tenant-a");
            assertThat(outboxEvent.getTransactionId()).isEqualTo(10L);
            assertThat(outboxEvent.isPublished()).isFalse();
            assertThat(outboxEvent.getAttemptCount()).isZero();
        });
    }

    @Test
    void publishPendingMarksEventPublishedAfterKafkaSend() {
        TransactionOutboxEvent outboxEvent = outboxEvent("transactions.realtime");
        when(outboxRepository.findTop50ByPublishedFalseAndNextRetryAtBeforeOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(outboxEvent));

        transactionOutboxService.publishPending();

        verify(transactionEventProducer).sendToTopic(org.mockito.Mockito.eq("transactions.realtime"), any(TransactionEvent.class));
        assertThat(outboxEvent.isPublished()).isTrue();
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPendingKeepsEventPendingWhenKafkaSendFails() {
        TransactionOutboxEvent outboxEvent = outboxEvent("transactions.realtime");
        when(outboxRepository.findTop50ByPublishedFalseAndNextRetryAtBeforeOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(outboxEvent));
        doThrow(new IllegalStateException("kafka unavailable"))
                .when(transactionEventProducer).sendToTopic(org.mockito.Mockito.eq("transactions.realtime"), any(TransactionEvent.class));

        transactionOutboxService.publishPending();

        assertThat(outboxEvent.isPublished()).isFalse();
        assertThat(outboxEvent.getAttemptCount()).isEqualTo(1);
        assertThat(outboxEvent.getLastError()).isEqualTo("kafka unavailable");
        assertThat(outboxEvent.getNextRetryAt()).isAfter(LocalDateTime.now());
    }

    private TransactionOutboxEvent outboxEvent(String topic) {
        return TransactionOutboxEvent.builder()
                .tenantId("tenant-a")
                .transactionId(10L)
                .topic(topic)
                .payload(serialize(event()))
                .published(false)
                .attemptCount(0)
                .nextRetryAt(LocalDateTime.now().minusSeconds(1))
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    private TransactionEvent event() {
        return new TransactionEvent(
                "tenant-a",
                10L,
                20L,
                5000.0,
                "Seoul",
                "MOBILE",
                LocalDateTime.of(2026, 8, 24, 10, 0),
                "MOBILE"
        );
    }

    private String serialize(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
