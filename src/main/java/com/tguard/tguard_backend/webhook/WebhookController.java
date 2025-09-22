package com.tguard.tguard_backend.webhook;

import com.tguard.tguard_backend.transaction.service.TransactionService;
import com.tguard.tguard_backend.webhook.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final TransactionService transactionService;

    @PostMapping("/payment")
    public ResponseEntity<Void> receive(@RequestBody PaymentEvent e) {
        if (!"APPROVED".equalsIgnoreCase(e.status())) return ResponseEntity.ok().build();

        var payload = new TransactionService.WebhookPayload(
                e.last4(), e.brand(), e.amount(), e.currency(),
                e.merchant(), e.channel(), e.occurredAt()
        );
        transactionService.createFromWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
