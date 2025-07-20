package com.tguard.tguard_backend.detection.dto;

import com.tguard.tguard_backend.transaction.dto.TransactionResponse;

import java.time.LocalDateTime;

public record DetectionResultResponse(
        Long id,
        Long transactionId,
        Boolean isSuspicious,
        String reason,
        LocalDateTime detectedAt,
        TransactionResponse transaction
) {
}
