package com.tguard.tguard_backend.webhook.dto;

import java.time.LocalDateTime;

public record PaymentWebhookEvent(
        String eventId,
        String status,
        String brand,
        String last4,
        Long amount,
        String currency,
        String merchant,
        String channel,
        String deviceInfo,
        LocalDateTime occurredAt,
        String paymentKey,
        String orderId,
        String rawPayload,
        String testPhoneNumber
) {
    public PaymentWebhookEvent withRawPayload(String rawPayload) {
        return new PaymentWebhookEvent(
                this.eventId, this.status, this.brand, this.last4, this.amount,
                this.currency, this.merchant, this.channel, this.deviceInfo, this.occurredAt,
                this.paymentKey, this.orderId, rawPayload, this.testPhoneNumber);
    }
}
