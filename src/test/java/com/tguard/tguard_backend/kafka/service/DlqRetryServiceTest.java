package com.tguard.tguard_backend.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.common.pipeline.PipelineProperties;
import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.kafka.entity.DlqTransactionRetry;
import com.tguard.tguard_backend.kafka.producer.TransactionEventProducer;
import com.tguard.tguard_backend.kafka.repository.DlqTransactionRetryRepository;
import com.tguard.tguard_backend.common.notification.SlackNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DlqRetryServiceTest {

    @Mock
    private DlqTransactionRetryRepository dlqTransactionRetryRepository;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @Mock
    private SlackNotifier slackNotifier;

    private DlqRetryService dlqRetryService;

    @BeforeEach
    void setUp() {
        PipelineProperties.DlqProperties dlqProps = new PipelineProperties.DlqProperties(3, 60, 2);
        PipelineProperties pipelineProperties = new PipelineProperties(dlqProps, null, null);
        dlqRetryService = new DlqRetryService(
                dlqTransactionRetryRepository,
                transactionEventProducer,
                pipelineProperties,
                slackNotifier,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("경고 임계치 도달 시 Slack 경고가 한 번 발송되고 시도 횟수가 기록된다")
    void warnsOnceBeforeExhaustion() {
        DlqTransactionRetry retry = DlqTransactionRetry.builder()
                .tenantId("tenant-1")
                .transactionId(42L)
                .payload("{}")
                .attemptCount(1)
                .nextRetryAt(LocalDateTime.now())
                .lastFailedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .errorMessage("before")
                .errorType("IllegalStateException")
                .lastAlertedAttempt(0)
                .build();

        when(dlqTransactionRetryRepository.findByTenantIdAndTransactionId("tenant-1", 42L))
                .thenReturn(Optional.of(retry));
        when(dlqTransactionRetryRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DlqEvent dlqEvent = new DlqEvent(
                new TransactionEvent("tenant-1", 42L, null, null, null, null, null, null),
                "boom",
                "IllegalStateException",
                LocalDateTime.now()
        );

        dlqRetryService.registerFailure(dlqEvent);

        verify(slackNotifier, times(1)).sendMessage(argThat(message -> message.contains("warning")));
        ArgumentCaptor<DlqTransactionRetry> captor = ArgumentCaptor.forClass(DlqTransactionRetry.class);
        verify(dlqTransactionRetryRepository).save(captor.capture());
        assertThat(captor.getValue().getLastAlertedAttempt()).isEqualTo(2);
        assertThat(retry.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("최대 재시도 초과 시 경고를 발송하고 엔티티를 제거한다")
    void notifiesWhenMaxAttemptsExceeded() {
        DlqTransactionRetry retry = DlqTransactionRetry.builder()
                .tenantId("tenant-1")
                .transactionId(99L)
                .payload("{}")
                .attemptCount(2)
                .nextRetryAt(LocalDateTime.now())
                .lastFailedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .errorMessage("fail")
                .errorType("RuntimeException")
                .lastAlertedAttempt(2)
                .build();

        when(dlqTransactionRetryRepository.findByTenantIdAndTransactionId("tenant-1", 99L))
                .thenReturn(Optional.of(retry));

        DlqEvent dlqEvent = new DlqEvent(
                new TransactionEvent("tenant-1", 99L, null, null, null, null, null, null),
                "boom",
                "RuntimeException",
                LocalDateTime.now()
        );

        dlqRetryService.registerFailure(dlqEvent);

        verify(slackNotifier).sendMessage(argThat(message -> message.contains(":rotating_light:")));
        verify(dlqTransactionRetryRepository).delete(retry);
        verify(transactionEventProducer, never()).send(any());
    }
}
