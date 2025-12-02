package com.tguard.tguard_backend.kafka.consumer;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.detection.service.DetectionResultService;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.kafka.producer.DlqProducer;
import com.tguard.tguard_backend.transaction.entity.Channel;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("!test")
public class TransactionEventConsumer {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final DetectionResultService detectionResultService;
    private final DlqProducer dlqProducer;

    @KafkaListener(topics = "#{@kafkaTopicProperties.realtime()}", groupId = "fraud-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event) {
        String tenantId = event.tenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            log.info("Realtime transaction received for tenant {}: {}", tenantId, event);
            Transaction transaction = resolveTransaction(event, tenantId);
            detectionResultService.analyzeAndSave(tenantId, transaction);
        } catch (Exception e) {
            log.error("Realtime processing failed - DLQ dispatch: {}", event, e);
            dlqProducer.sendToDlq(event, e);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Transaction resolveTransaction(TransactionEvent event, String tenantId) {
        if (event.transactionId() != null) {
            return transactionRepository.findByIdAndTenantId(event.transactionId(), tenantId)
                    .orElseGet(() -> createTransaction(event, tenantId));
        }
        return createTransaction(event, tenantId);
    }

    private Transaction createTransaction(TransactionEvent event, String tenantId) {
        User user = userRepository.findByIdAndTenantId(event.userId(), tenantId)
                .orElseThrow(() -> new IllegalStateException("User not found for tenant %s: %s".formatted(tenantId, event.userId())));

        Transaction transaction = Transaction.builder()
                .tenantId(tenantId)
                .user(user)
                .amount(event.amount())
                .location(event.location())
                .deviceInfo(event.deviceInfo())
                .transactionTime(resolveTransactionTime(event.transactionTime()))
                .status(Transaction.Status.PENDING)
                .channel(resolveChannel(event.channel()))
                .build();

        return transactionRepository.save(transaction);
    }

    private Channel resolveChannel(String channel) {
        if (channel == null) {
            return Channel.UNKNOWN;
        }
        try {
            return Channel.valueOf(channel);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown channel '{}' received from transaction event", channel);
            return Channel.UNKNOWN;
        }
    }

    private LocalDateTime resolveTransactionTime(LocalDateTime transactionTime) {
        if (transactionTime == null) {
            return LocalDateTime.now();
        }
        return transactionTime;
    }
}
