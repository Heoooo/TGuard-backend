package com.tguard.tguard_backend.webhook;

import com.tguard.tguard_backend.transaction.service.TransactionService;
import com.tguard.tguard_backend.webhook.dto.PaymentWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/0") // 테스트 코드와 일관성을 위해 'webhooks'로 변경
@RequiredArgsConstructor
public class WebhookController {

    private final TransactionService transactionService;

    @PostMapping("/payment")
    public ResponseEntity<Void> receive(@RequestBody PaymentWebhookEvent event) {
        if (!"APPROVED".equalsIgnoreCase(event.status())) {
            return ResponseEntity.ok().build(); // 승인된 건만 저장
        }

        transactionService.recordFromWebhook(event);
        return ResponseEntity.ok().build();
    }
}

