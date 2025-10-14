package com.tguard.tguard_backend.kafka.consumer;

import com.tguard.tguard_backend.common.notification.SlackNotifier;
import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import com.tguard.tguard_backend.kafka.service.DlqRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqEventConsumer {

    private final SlackNotifier slackNotifier;
    private final DlqRetryService dlqRetryService;

    @KafkaListener(topics = "transactions-dlq", groupId = "fraud-dlq-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDlq(DlqEvent dlqEvent) {
        log.warn("DLQ message received: {}", dlqEvent);

        String alertMessage = String.format(
                ":warning: DLQ event captured. transactionId=%s tenant=%s amount=%.2f channel=%s location=%s time=%s error=%s (%s)",
                dlqEvent.originalEvent().transactionId(),
                dlqEvent.originalEvent().tenantId(),
                dlqEvent.originalEvent().amount(),
                dlqEvent.originalEvent().channel(),
                dlqEvent.originalEvent().location(),
                dlqEvent.originalEvent().transactionTime(),
                dlqEvent.errorType(),
                dlqEvent.errorMessage()
        );

        slackNotifier.sendMessage(alertMessage);
        dlqRetryService.registerFailure(dlqEvent);
    }
}
