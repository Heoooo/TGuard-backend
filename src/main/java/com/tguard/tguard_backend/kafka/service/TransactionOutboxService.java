package com.tguard.tguard_backend.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.kafka.entity.TransactionOutboxEvent;
import com.tguard.tguard_backend.kafka.producer.TransactionEventProducer;
import com.tguard.tguard_backend.kafka.repository.TransactionOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionOutboxService {

    private static final int BASE_RETRY_DELAY_SECONDS = 30;

    private final TransactionOutboxEventRepository outboxRepository;
    private final TransactionEventProducer transactionEventProducer;
    private final KafkaTopicProperties topicProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(TransactionEvent event) {
        enqueue(event, topicProperties.realtime());
        enqueue(event, topicProperties.batch());
    }

    private void enqueue(TransactionEvent event, String topic) {
        outboxRepository.save(TransactionOutboxEvent.builder()
                .tenantId(event.tenantId())
                .transactionId(event.transactionId())
                .topic(topic)
                .payload(serialize(event))
                .published(false)
                .attemptCount(0)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Scheduled(fixedDelayString = "${tguard.outbox.publish-fixed-delay:PT10S}")
    @Transactional
    public void publishPending() {
        List<TransactionOutboxEvent> pendingEvents = outboxRepository
                .findTop50ByPublishedFalseAndNextRetryAtBeforeOrderByCreatedAtAsc(LocalDateTime.now());

        for (TransactionOutboxEvent outboxEvent : pendingEvents) {
            try {
                TransactionEvent event = deserialize(outboxEvent.getPayload());
                transactionEventProducer.sendToTopic(outboxEvent.getTopic(), event);
                outboxEvent.markPublished(LocalDateTime.now());
            } catch (RuntimeException ex) {
                LocalDateTime nextRetryAt = nextRetryAt(outboxEvent.getAttemptCount() + 1);
                outboxEvent.markFailed(ex.getMessage(), nextRetryAt);
                log.warn("Transaction outbox publish failed id={} tenant={} transaction={}",
                        outboxEvent.getId(), outboxEvent.getTenantId(), outboxEvent.getTransactionId(), ex);
            }
        }
    }

    private LocalDateTime nextRetryAt(int attempt) {
        long delaySeconds = (long) BASE_RETRY_DELAY_SECONDS * Math.max(1, attempt);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    private String serialize(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transaction outbox event", e);
        }
    }

    private TransactionEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, TransactionEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize transaction outbox event", e);
        }
    }
}
