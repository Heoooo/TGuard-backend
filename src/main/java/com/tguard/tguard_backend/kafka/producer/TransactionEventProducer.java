package com.tguard.tguard_backend.kafka.producer;

import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void send(TransactionEvent event) {
        Timer.Sample overall = Timer.start(Metrics.globalRegistry);
        try {
            sendToTopic(topicProperties.realtime(), event);
            sendToTopic(topicProperties.batch(), event);

            log.debug("Transaction event dispatched for tenant {} to realtime={} and batch={} pipelines",
                    event.tenantId(), topicProperties.realtime(), topicProperties.batch());
        } finally {
            overall.stop(timer("transaction.kafka.send.duration", "stage", "producer"));
        }
    }

    public void sendToTopic(String topic, TransactionEvent event) {
        timer("transaction.kafka.send", "topic", topic)
                .record(() -> waitForSend(kafkaTemplate.send(topic, event)));
        Metrics.counter("tguard.events.kafka.publish.attempts", "topic", topic).increment();
    }

    private SendResult<String, TransactionEvent> waitForSend(CompletableFuture<SendResult<String, TransactionEvent>> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish transaction event to Kafka", e);
        }
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .tags(tags)
                .register(Metrics.globalRegistry);
    }
}
