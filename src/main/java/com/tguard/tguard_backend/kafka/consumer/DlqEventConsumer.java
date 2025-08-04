package com.tguard.tguard_backend.kafka.consumer;

import com.tguard.tguard_backend.common.notification.SlackNotifier;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
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
    public void consumeDlq(TransactionEvent event) {
        log.warn("DLQ 수신: {}", event);

        String alertMessage = String.format(
                ":warning: *DLQ 발생 알림*\n거래 ID: %d\n유저 ID: %d\n금액: %.2f\n채널: %s\n위치: %s\n시간: %s",
                event.transactionId(),
                event.userId(),
                event.amount(),
                event.channel(),
                event.location(),
                event.transactionTime()
        );

        slackNotifier.sendMessage(alertMessage);
    }
}
