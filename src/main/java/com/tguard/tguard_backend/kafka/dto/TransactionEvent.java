package com.tguard.tguard_backend.kafka.dto;

import java.time.LocalDateTime;

public record TransactionEvent(
        String tenantId,
        Long transactionId,
        Long userId,
        Double amount,
        String location,
        String deviceInfo,
        LocalDateTime transactionTime,
        String channel
) {
}
