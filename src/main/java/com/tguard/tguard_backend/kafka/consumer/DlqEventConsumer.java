package com.tguard.tguard_backend.kafka.consumer;

import com.tguard.tguard_backend.common.notification.SlackNotifier;
import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqEventConsumer {

    private final SlackNotifier slackNotifier;

    @KafkaListener(topics = "transactions-dlq", groupId = "fraud-dlq-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDlq(DlqEvent dlqEvent) {
        log.warn("DLQ 수신: {}", dlqEvent);

        String alertMessage = String.format(
                ":warning: *DLQ 발생 알림*\n거래 ID: %d\n유저 ID: %d\n금액: %.2f\n채널: %s\n위치: %s\n시간: %s\n에러: %s (%s)",
                dlqEvent.originalEvent().transactionId(),
                dlqEvent.originalEvent().userId(),
                dlqEvent.originalEvent().amount(),
                dlqEvent.originalEvent().channel(),
                dlqEvent.originalEvent().location(),
                dlqEvent.originalEvent().transactionTime(),
                dlqEvent.errorType(),
                dlqEvent.errorMessage()
        );

        slackNotifier.sendMessage(alertMessage);
    }
}
