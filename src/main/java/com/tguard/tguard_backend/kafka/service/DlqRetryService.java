package com.tguard.tguard_backend.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.common.notification.SlackNotifier;
import com.tguard.tguard_backend.common.pipeline.PipelineProperties;
import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.kafka.entity.DlqTransactionRetry;
import com.tguard.tguard_backend.kafka.producer.TransactionEventProducer;
import com.tguard.tguard_backend.kafka.repository.DlqTransactionRetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqRetryService {

    private final DlqTransactionRetryRepository dlqTransactionRetryRepository;
    private final TransactionEventProducer transactionEventProducer;
    private final PipelineProperties pipelineProperties;
    private final SlackNotifier slackNotifier;
    private final ObjectMapper objectMapper;

    @Transactional
    public void registerFailure(DlqEvent dlqEvent) {
        TransactionEvent original = dlqEvent.originalEvent();
        String tenantId = Optional.ofNullable(original.tenantId()).orElse("default");
        String payloadJson = serializeTransaction(original);

        DlqTransactionRetry retry = dlqTransactionRetryRepository
                .findByTenantIdAndTransactionId(tenantId, original.transactionId())
                .orElseGet(() -> DlqTransactionRetry.builder()
                        .tenantId(tenantId)
                        .transactionId(original.transactionId())
                        .payload(payloadJson)
                        .attemptCount(0)
                        .nextRetryAt(LocalDateTime.now())
                        .lastFailedAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .errorMessage(dlqEvent.errorMessage())
                        .errorType(dlqEvent.errorType())
                        .lastAlertedAttempt(0)
                        .build());

        retry.refreshPayload(payloadJson);

        int attemptsBefore = retry.getAttemptCount();
        LocalDateTime nextRetry = computeNextRetry(attemptsBefore + 1);
        retry.updateFailure(dlqEvent.errorMessage(), dlqEvent.errorType(), nextRetry);
        dlqTransactionRetryRepository.save(retry);

        maybeWarnBeforeExhaustion(retry);
        if (retry.getAttemptCount() >= pipelineProperties.dlq().maxAttempts()) {
            notifyMaxAttemptReached(retry);
        }
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void retryPending() {
        List<DlqTransactionRetry> candidates = dlqTransactionRetryRepository
                .findTop50ByNextRetryAtBeforeOrderByNextRetryAtAsc(LocalDateTime.now());
        if (candidates.isEmpty()) {
            return;
        }

        for (DlqTransactionRetry retry : candidates) {
            try {
                TransactionEvent event = deserializeTransaction(retry.getPayload());
                transactionEventProducer.send(event);
                dlqTransactionRetryRepository.delete(retry);
                log.info("Re-dispatched DLQ transaction for tenant={} transaction={}", retry.getTenantId(), retry.getTransactionId());
            } catch (Exception ex) {
                log.error("DLQ retry failed tenant={} transaction={}", retry.getTenantId(), retry.getTransactionId(), ex);
                LocalDateTime nextRetry = computeNextRetry(retry.getAttemptCount() + 1);
                retry.updateFailure(ex.getMessage(), ex.getClass().getSimpleName(), nextRetry);
                dlqTransactionRetryRepository.save(retry);

                if (retry.getAttemptCount() >= pipelineProperties.dlq().maxAttempts()) {
                    notifyMaxAttemptReached(retry);
                }
            }
        }
    }

    private LocalDateTime computeNextRetry(int attempt) {
        int baseDelay = Math.max(1, pipelineProperties.dlq().baseRetryDelaySeconds());
        long delaySeconds = (long) baseDelay * attempt;
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    private String serializeTransaction(TransactionEvent original) {
        try {
            return objectMapper.writeValueAsString(original);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transaction event", e);
        }
    }

    private TransactionEvent deserializeTransaction(String json) {
        try {
            return objectMapper.readValue(json, TransactionEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize transaction event", e);
        }
    }

    private void notifyMaxAttemptReached(DlqTransactionRetry retry) {
        String transactionId = transactionIdOrUnknown(retry);
        String alertMessage = String.format(
                ":rotating_light: DLQ retry attempts exceeded. tenant=%s transaction=%s attempts=%d lastError=%s (%s)",
                retry.getTenantId(),
                transactionId,
                retry.getAttemptCount(),
                retry.getErrorMessage(),
                retry.getErrorType()
        );
        slackNotifier.sendMessage(alertMessage);
        retry.markAlertedUpTo(pipelineProperties.dlq().maxAttempts());
        dlqTransactionRetryRepository.delete(retry);
    }

    private void maybeWarnBeforeExhaustion(DlqTransactionRetry retry) {
        int warnThreshold = pipelineProperties.dlq().warnAttempts();
        if (warnThreshold <= 0 || retry.getAttemptCount() < warnThreshold) {
            return;
        }
        if (retry.getAttemptCount() >= pipelineProperties.dlq().maxAttempts()) {
            return;
        }
        if (retry.getLastAlertedAttempt() >= warnThreshold) {
            return;
        }

        slackNotifier.sendMessage(String.format(
                ":warning: DLQ retry warning tenant=%s transaction=%s attempts=%d/%d lastError=%s (%s)",
                retry.getTenantId(),
                transactionIdOrUnknown(retry),
                retry.getAttemptCount(),
                pipelineProperties.dlq().maxAttempts(),
                retry.getErrorMessage(),
                retry.getErrorType()
        ));
        retry.markAlertedUpTo(warnThreshold);
    }

    @Transactional
    public void retrySingle(Long messageId) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }
        DlqTransactionRetry retry = dlqTransactionRetryRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DLQ message not found"));
        try {
            TransactionEvent event = deserializeTransaction(retry.getPayload());
            transactionEventProducer.send(event);
            dlqTransactionRetryRepository.delete(retry);
            log.info("Manually retried DLQ message id={} tenant={} transaction={}",
                    retry.getId(), retry.getTenantId(), retry.getTransactionId());
        } catch (Exception ex) {
            log.error("Manual DLQ retry failed id={} tenant={} transaction={}",
                    retry.getId(), retry.getTenantId(), retry.getTransactionId(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retry DLQ message", ex);
        }
    }

    private String transactionIdOrUnknown(DlqTransactionRetry retry) {
        Long transactionId = retry.getTransactionId();
        if (transactionId == null) {
            return "unknown";
        }
        return transactionId.toString();
    }
}
