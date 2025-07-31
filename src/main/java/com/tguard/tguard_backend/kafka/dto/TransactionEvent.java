package com.tguard.tguard_backend.kafka.dto;

import java.time.LocalDateTime;

public record TransactionEvent(
        Long transactionId,
        Long userId,
        Double amount,
        String location,
        String deviceInfo,
        LocalDateTime transactionTime,
        String channel
) {
}
