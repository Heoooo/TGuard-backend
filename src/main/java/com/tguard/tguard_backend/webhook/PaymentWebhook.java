package com.tguard.tguard_backend.webhook;

import com.tguard.tguard_backend.transaction.entity.Channel;
import org.json.JSONObject;

import java.time.LocalDateTime;

public record PaymentWebhook(
        String eventId,
        String status,
        JSONObject card,   // 그대로 둠
        Long amount,
        String currency,
        String merchant,
        String channel,
        String occurredAtRaw,
        String rawPayload
) {}