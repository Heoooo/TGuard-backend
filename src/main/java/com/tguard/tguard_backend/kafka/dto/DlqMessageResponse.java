package com.tguard.tguard_backend.kafka.dto;

import java.time.LocalDateTime;

public record DlqMessageResponse(
        Long id,
        String queue,
        String tenantId,
        Long transactionId,
        String payload,
        String errorMessage,
        String errorType,
        int attemptCount,
        LocalDateTime createdAt,
        LocalDateTime lastFailedAt,
        LocalDateTime nextRetryAt
) {
}
