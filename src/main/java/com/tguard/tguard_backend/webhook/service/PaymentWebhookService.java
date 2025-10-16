package com.tguard.tguard_backend.webhook.service;

import com.tguard.tguard_backend.transaction.service.TransactionService;
import com.tguard.tguard_backend.webhook.WebhookIdempotencyStore;
import com.tguard.tguard_backend.webhook.adapter.PaymentAdapter;
import com.tguard.tguard_backend.webhook.dto.PaymentWebhookEvent;
import com.tguard.tguard_backend.webhook.filter.WebhookSignatureVerifier;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Transactional
    public void handle(String rawBody, String idemKey) {
        // 1) 공급자별 payload → 공통 DTO
        PaymentWebhookEvent event = paymentAdapter.toCommonEvent(rawBody);

        // 2) 멱등성(이벤트ID 우선), 보조로 Idempotency-Key
        String idemKeyFinal = (idemKey != null && !idemKey.isBlank()) ? idemKey : event.eventId();
        if (!idempotencyStore.markIfFirstSeen(idemKeyFinal)) return;

        // 3) Transaction 저장 + 후속 처리(이상탐지→문자발송)
        transactionService.recordFromWebhook(event);
    }
}

