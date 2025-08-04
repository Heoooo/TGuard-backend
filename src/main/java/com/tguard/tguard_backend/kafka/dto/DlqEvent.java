package com.tguard.tguard_backend.kafka.dto;

import java.time.LocalDateTime;

public record DlqEvent(
        TransactionEvent originalEvent,
        String errorMessage,
        String errorType,
        LocalDateTime failedAt) {
}
