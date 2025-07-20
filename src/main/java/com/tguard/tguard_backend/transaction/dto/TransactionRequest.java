package com.tguard.tguard_backend.transaction.dto;

import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        @NotNull Double amount,
        String location,
        String deviceInfo
) {
}
