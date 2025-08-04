package com.tguard.tguard_backend.transaction.dto;

import com.tguard.tguard_backend.transaction.entity.Channel;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        Long userId,
        @NotNull Double amount,
        String location,
        String deviceInfo,
        @NotNull Channel channel
) {
}
