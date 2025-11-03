package com.tguard.tguard_backend.kafka.producer;

import com.tguard.tguard_backend.kafka.config.KafkaTopicProperties;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
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
        Timer.Sample overall = Timer.start(Metrics.globalRegistry);
        try {
            timer("transaction.kafka.send", "topic", topicProperties.realtime())
                    .record(() -> kafkaTemplate.send(topicProperties.realtime(), event));
            timer("transaction.kafka.send", "topic", topicProperties.batch())
                    .record(() -> kafkaTemplate.send(topicProperties.batch(), event));

            log.debug("Transaction event dispatched for tenant {} to realtime={} and batch={} pipelines",
                    event.tenantId(), topicProperties.realtime(), topicProperties.batch());
        } finally {
            overall.stop(timer("transaction.kafka.send.duration", "stage", "producer"));
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
