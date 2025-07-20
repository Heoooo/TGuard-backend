package com.tguard.tguard_backend.transaction.dto;

import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long userId,
        Double amount,
        String location,
        String deviceInfo,
        LocalDateTime transactionTime,
        String status
) {
}
