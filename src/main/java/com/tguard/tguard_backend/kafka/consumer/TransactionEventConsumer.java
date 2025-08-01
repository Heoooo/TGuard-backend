package com.tguard.tguard_backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class TransactionEventConsumer {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final DetectionResultService detectionResultService;
    private final DlqProducer dlqProducer;

    @KafkaListener(topics = "transactions", groupId = "fraud-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event) {
        try {
            log.info("Kafka 수신: {}", event);

            User user = userRepository.findById(event.userId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + event.userId()));

            Transaction transaction = Transaction.builder()
                    .user(user)
                    .amount(event.amount())
                    .location(event.location())
                    .deviceInfo(event.deviceInfo())
                    .transactionTime(event.transactionTime() != null ? event.transactionTime() : LocalDateTime.now())
                    .status(Transaction.Status.PENDING)
                    .channel(Channel.valueOf(event.channel()))
                    .build();

            transactionRepository.save(transaction);
            detectionResultService.analyzeAndSave(transaction);

        } catch (Exception e) {
            log.error("Kafka 처리 실패 - DLQ 전송: {}", event, e);
            dlqProducer.sendToDlq(event);
        }
    }
}
