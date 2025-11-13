package com.tguard.tguard_backend.webhook.adapter;

import com.tguard.tguard_backend.webhook.dto.PaymentWebhookEvent;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class PaymentAdapter {
    public PaymentWebhookEvent toCommonEvent(String rawBody) {
        JSONObject o = new JSONObject(rawBody);

        String eventId   = o.optString("event_id", o.optString("eventId"));
        String status    = o.optString("status");
        String currency  = o.optString("currency", "KRW");
        String merchant  = o.optString("merchant", o.optString("location", null));
        String channel   = o.optString("channel", "ONLINE");
        String deviceInfo = o.optString("deviceInfo", null);
        Long amount      = o.optLong("amount");

        String brand = null, last4 = null;
        if (o.has("card")) {
            JSONObject card = o.getJSONObject("card");
            brand = card.optString("brand");
            last4 = card.optString("last4");
        }

        String occurredAtStr = o.optString("occurred_at", o.optString("approvedAt"));
        LocalDateTime occurredAt = null;
        if (occurredAtStr != null && !occurredAtStr.isBlank()) {
            occurredAt = OffsetDateTime.parse(occurredAtStr).toLocalDateTime();
        }

        String paymentKey = o.optString("paymentKey", null);
        String orderId    = o.optString("orderId", null);
        String testPhoneNumber = o.optString("testPhoneNumber", o.optString("test_phone_number", null));

        return new PaymentWebhookEvent(
                eventId, status, brand, last4, amount, currency, merchant, channel, deviceInfo, occurredAt,
                paymentKey, orderId, rawBody, testPhoneNumber
        );
    }
}
