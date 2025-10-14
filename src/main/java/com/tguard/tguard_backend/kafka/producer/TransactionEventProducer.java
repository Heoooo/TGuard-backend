package com.tguard.tguard_backend.kafka.producer;

import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void send(TransactionEvent event) {
        kafkaTemplate.send(topicProperties.realtime(), event);
        kafkaTemplate.send(topicProperties.batch(), event);
        log.debug("Transaction event dispatched for tenant {} to realtime={} and batch={} pipelines",
                event.tenantId(), topicProperties.realtime(), topicProperties.batch());
    }
}
