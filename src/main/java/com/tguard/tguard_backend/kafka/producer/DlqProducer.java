package com.tguard.tguard_backend.kafka.producer;

import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DlqProducer {
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void sendToDlq(TransactionEvent event) {
        kafkaTemplate.send("transactions-dlq", event);
    }
}
