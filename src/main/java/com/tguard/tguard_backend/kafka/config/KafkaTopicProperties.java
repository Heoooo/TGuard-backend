package com.tguard.tguard_backend.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tguard.kafka.topics")
public record KafkaTopicProperties(
        String realtime,
        String batch,
        String dlq
) {
}
