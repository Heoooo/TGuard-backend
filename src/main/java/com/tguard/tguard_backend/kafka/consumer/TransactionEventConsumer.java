package com.tguard.tguard_backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.detection.service.DetectionResultService;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.transaction.entity.Channel;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TransactionEventConsumer {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final DetectionResultService detectionResultService;

    @KafkaListener(topics = "transactions", groupId = "fraud-group")
    public void consume(TransactionEvent event) {
        log.info("Kafka 수신: {}", event);

        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new RuntimeException("User not found: " + event.userId()));

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(event.amount())
                .location(event.location())
                .deviceInfo(event.deviceInfo())
                .transactionTime(event.transactionTime())
                .status(Transaction.Status.PENDING)
                .channel(Channel.valueOf(event.channel()))
                .build();

        transactionRepository.save(transaction);

        detectionResultService.analyzeAndSave(transaction);

    }
}
