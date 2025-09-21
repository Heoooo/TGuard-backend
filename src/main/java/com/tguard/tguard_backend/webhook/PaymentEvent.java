package com.tguard.tguard_backend.webhook;

import com.tguard.tguard_backend.transaction.entity.Channel;

import java.time.LocalDateTime;

public record PaymentEvent(
        String last4, String brand,
        Double amount, String currency,
        String merchant,
        Channel channel,
        LocalDateTime occurredAt,
        String status
) {}