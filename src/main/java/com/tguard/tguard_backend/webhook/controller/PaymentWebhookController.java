package com.tguard.tguard_backend.webhook.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.transaction.dto.TransactionResponse;
import com.tguard.tguard_backend.webhook.service.PaymentWebhookService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @Timed(value = "webhook.receive", extraTags = {"layer", "controller"})
    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receive(@RequestBody String rawBody,
                                                                    @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
                                                                    @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        try {
            webhookService.verifySignatureOrThrow(rawBody, signature);

            Map<String, Object> payload = new HashMap<>();

            var result = webhookService.handle(rawBody, idemKey);
            if (result.isPresent()) {
                TransactionResponse response = result.get();
                payload.put("status", "SUCCESS");
                payload.put("message", "Webhook processed successfully");
                payload.put("transactionId", response.id());
            } else {
            payload.put("status", "IGNORED");
            payload.put("message", "Webhook ignored due to duplicate idempotency key");
            }

            return ResponseEntity.ok(ApiResponse.success(payload));
        } finally {
            sample.stop(timer("webhook.receive.manual", "layer", "controller"));
        }
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .tags(tags)
                .register(Metrics.globalRegistry);
    }
}
