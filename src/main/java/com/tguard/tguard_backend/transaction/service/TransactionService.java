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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

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
        Timer.Sample overall = Timer.start(Metrics.globalRegistry);
        try {
            String tenantId = TenantContextHolder.requireTenantId();

            Transaction existing = recordTimer("transaction.record.lookup", () -> findExistingTransaction(event, tenantId),
                    "stage", "duplicate-check");
            if (existing != null) {
                return transactionMapper.toResponse(existing);
            }

            Transaction mapped = recordTimer("transaction.record.map",
                    () -> webhookMapper.mapToTransaction(event, tenantId),
                    "stage", "mapper");

            Transaction saved = recordTimer("transaction.record.save",
                    () -> transactionRepository.save(mapped),
                    "stage", "persist");

            recordTimer("transaction.record.publish", () -> {
                publishTransactionEvent(saved);
                return null;
            }, "stage", "kafka");

            return transactionMapper.toResponse(saved);
        } finally {
            overall.stop(timer("transaction.record.duration", "stage", "overall"));
        }
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

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(Pageable pageable) {
        String tenantId = TenantContextHolder.requireTenantId();
        return transactionRepository.findByTenantId(tenantId, pageable)
                .map(transactionMapper::toResponse);
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

    private Timer timer(String name, String... tags) {
        return Timer.builder(name)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .tags(tags)
                .register(Metrics.globalRegistry);
    }

    private <T> T recordTimer(String name, Supplier<T> supplier, String... tags) {
        return timer(name, tags).record(supplier);
    }
}
