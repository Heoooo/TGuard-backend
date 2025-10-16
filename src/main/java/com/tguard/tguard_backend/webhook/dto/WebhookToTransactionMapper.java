package com.tguard.tguard_backend.webhook.dto;

import com.tguard.tguard_backend.card.entity.Card;
import com.tguard.tguard_backend.card.repository.CardRepository;
import com.tguard.tguard_backend.transaction.entity.Channel;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookToTransactionMapper {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public Transaction mapToTransaction(PaymentWebhookEvent event, String tenantId) {
        User user = resolveUser(event, tenantId);

        Transaction.Status status = switch (String.valueOf(event.status()).toUpperCase()) {
            case "APPROVED", "DONE", "SUCCESS" -> Transaction.Status.APPROVED;
            case "DECLINED", "CANCELED", "FAIL" -> Transaction.Status.REJECTED;
            default -> Transaction.Status.PENDING;
        };

        Channel channel = switch (String.valueOf(event.channel()).toUpperCase()) {
            case "ONLINE", "WEB" -> Channel.WEB;
            case "OFFLINE", "POS" -> Channel.POS;
            case "MOBILE" -> Channel.MOBILE;
            case "KIOSK" -> Channel.KIOSK;
            default -> Channel.UNKNOWN;
        };

        Double amount = event.amount() != null ? event.amount().doubleValue() : null;
        LocalDateTime occurredAt = event.occurredAt() != null ? event.occurredAt() : LocalDateTime.now();

        return Transaction.builder()
                .tenantId(tenantId)
                .user(user)
                .amount(amount)
                .location(event.merchant())
                .deviceInfo(event.deviceInfo())
                .transactionTime(occurredAt)
                .status(status)
                .channel(channel)
                .externalEventId(hasText(event.eventId()) ? event.eventId() : null)
                .paymentKey(event.paymentKey())
                .orderId(event.orderId())
                .currency(event.currency())
                .rawPayload(event.rawPayload())
                .build();
    }

    private User resolveUser(PaymentWebhookEvent event, String tenantId) {
        return resolveByCard(event, tenantId)
                .or(() -> resolveByOrderId(event, tenantId))
                .orElseThrow(() -> new IllegalArgumentException("user not found for webhook event"));
    }

    private Optional<User> resolveByCard(PaymentWebhookEvent event, String tenantId) {
        if (!hasText(event.brand()) || !hasText(event.last4())) {
            return Optional.empty();
        }

        return cardRepository.findByOwner_TenantIdAndBrandIgnoreCaseAndLast4(tenantId, event.brand().trim(), event.last4().trim())
                .map(Card::getOwner);
    }

    private Optional<User> resolveByOrderId(PaymentWebhookEvent event, String tenantId) {
        if (!hasText(event.orderId())) {
            return Optional.empty();
        }

        String orderId = event.orderId().trim();
        if (orderId.startsWith("U:")) {
            String[] tokens = orderId.split(":", 3);
            if (tokens.length >= 2 && hasText(tokens[1])) {
                return userRepository.findByUsernameAndTenantId(tokens[1].trim(), tenantId);
            }
        }
        return Optional.empty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
