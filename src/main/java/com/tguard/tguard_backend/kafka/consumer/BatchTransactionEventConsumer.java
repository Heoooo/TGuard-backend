package com.tguard.tguard_backend.kafka.consumer;

import com.tguard.tguard_backend.batch.entity.BatchTransactionEvent;
import com.tguard.tguard_backend.batch.repository.BatchTransactionEventRepository;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BatchTransactionEventConsumer {

    private final BatchTransactionEventRepository batchTransactionEventRepository;

    @KafkaListener(topics = "#{@kafkaTopicProperties.batch()}", groupId = "fraud-batch-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TransactionEvent event) {
        BatchTransactionEvent entity = BatchTransactionEvent.builder()
                .tenantId(event.tenantId())
                .transactionId(event.transactionId())
                .userId(event.userId())
                .amount(event.amount())
                .location(event.location())
                .deviceInfo(event.deviceInfo())
                .channel(event.channel())
                .transactionTime(event.transactionTime())
                .receivedAt(LocalDateTime.now())
                .processed(false)
                .build();

        batchTransactionEventRepository.save(entity);
        log.debug("Buffered transaction {} for batch processing (tenant={})", event.transactionId(), event.tenantId());
    }
}
