package com.tguard.tguard_backend.webhook.controller;

import com.tguard.tguard_backend.webhook.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @PostMapping("/payments")
    public ResponseEntity<Void> receive(@RequestBody String rawBody,
                                        @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        webhookService.verifySignatureOrThrow(rawBody, signature);

        webhookService.handle(rawBody, idemKey);

        return ResponseEntity.ok().build();
    }
}



