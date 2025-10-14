package com.tguard.tguard_backend.kafka.producer;

import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class DlqProducer {

    private final KafkaTemplate<String, DlqEvent> dlqKafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void sendToDlq(TransactionEvent event, Exception exception) {
        DlqEvent dlqEvent = new DlqEvent(
                event,
                exception != null ? exception.getMessage() : null,
                exception != null ? exception.getClass().getSimpleName() : null,
                LocalDateTime.now()
        );
        dlqKafkaTemplate.send(topicProperties.dlq(), dlqEvent);
    }
}
