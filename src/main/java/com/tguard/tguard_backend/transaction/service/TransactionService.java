package com.tguard.tguard_backend.transaction.service;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.kafka.producer.TransactionEventProducer;
import com.tguard.tguard_backend.transaction.dto.TransactionMapper;
import com.tguard.tguard_backend.transaction.dto.TransactionRequest;
import com.tguard.tguard_backend.transaction.dto.TransactionResponse;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.transaction.exception.TransactionNotFoundException;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.exception.UserNotFoundException;
import com.tguard.tguard_backend.user.repository.UserRepository;
import com.tguard.tguard_backend.webhook.dto.PaymentWebhookEvent;
import com.tguard.tguard_backend.webhook.dto.WebhookToTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionEventProducer transactionEventProducer;
    private final WebhookToTransactionMapper webhookMapper;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse recordFromWebhook(PaymentWebhookEvent event) {
        String tenantId = TenantContextHolder.requireTenantId();
        Transaction existing = findExistingTransaction(event, tenantId);
        if (existing != null) {
            return transactionMapper.toResponse(existing);
        }

        Transaction saved = transactionRepository.save(webhookMapper.mapToTransaction(event, tenantId));
        publishTransactionEvent(saved);

        return transactionMapper.toResponse(saved);
    }

    private Transaction findExistingTransaction(PaymentWebhookEvent event, String tenantId) {
        if (hasText(event.eventId())) {
            Optional<Transaction> existing = transactionRepository.findByTenantIdAndExternalEventId(tenantId, event.eventId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (hasText(event.paymentKey())) {
            Optional<Transaction> existing = transactionRepository.findByTenantIdAndPaymentKey(tenantId, event.paymentKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (hasText(event.orderId())) {
            Optional<Transaction> existing = transactionRepository.findByTenantIdAndOrderId(tenantId, event.orderId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return null;
    }

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        String tenantId = TenantContextHolder.requireTenantId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(UserNotFoundException::new);

        Transaction transaction = Transaction.builder()
                .tenantId(tenantId)
                .user(user)
                .amount(request.amount())
                .location(request.location())
                .deviceInfo(request.deviceInfo())
                .channel(request.channel())
                .transactionTime(LocalDateTime.now())
                .status(Transaction.Status.PENDING)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        publishTransactionEvent(saved);

        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        String tenantId = TenantContextHolder.requireTenantId();
        Transaction transaction = transactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(TransactionNotFoundException::new);
        return transactionMapper.toResponse(transaction);
    }

    private void publishTransactionEvent(Transaction tx) {
        TransactionEvent event = new TransactionEvent(
                tx.getTenantId(),
                tx.getId(),
                tx.getUser().getId(),
                tx.getAmount(),
                tx.getLocation(),
                tx.getDeviceInfo(),
                tx.getTransactionTime(),
                tx.getChannel().name()
        );
        transactionEventProducer.send(event);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
