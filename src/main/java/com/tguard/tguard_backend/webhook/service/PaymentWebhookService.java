package com.tguard.tguard_backend.webhook.service;

import com.tguard.tguard_backend.transaction.dto.TransactionResponse;
import com.tguard.tguard_backend.transaction.service.TransactionService;
import com.tguard.tguard_backend.webhook.WebhookIdempotencyStore;
import com.tguard.tguard_backend.webhook.adapter.PaymentAdapter;
import com.tguard.tguard_backend.webhook.dto.PaymentWebhookEvent;
import com.tguard.tguard_backend.webhook.filter.WebhookSignatureVerifier;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookIdempotencyStore idempotencyStore;
    private final PaymentAdapter paymentAdapter;
    private final TransactionService transactionService;

    public void verifySignatureOrThrow(String rawBody, String signature) {
        if (!signatureVerifier.verify(rawBody, signature)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }
    }

    @Timed("webhook.handle")
    @Transactional
    public Optional<TransactionResponse> handle(String rawBody, String idemKey) {
        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        try {
            PaymentWebhookEvent event = paymentAdapter.toCommonEvent(rawBody);

            String idemKeyFinal = idemKey;
            if (idemKeyFinal == null || idemKeyFinal.isBlank()) {
                idemKeyFinal = event.eventId();
            }
            if (!idempotencyStore.markIfFirstSeen(idemKeyFinal)) {
                return Optional.empty();
            }

            TransactionResponse response = transactionService.recordFromWebhook(event);
            return Optional.ofNullable(response);
        } finally {
            sample.stop(timer("webhook.handle.manual", "layer", "service"));
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
