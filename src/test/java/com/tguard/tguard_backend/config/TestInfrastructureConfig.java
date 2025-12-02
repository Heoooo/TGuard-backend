package com.tguard.tguard_backend.config;

import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Profile("test")
public class TestInfrastructureConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplateString() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    public KafkaTemplate<String, TransactionEvent> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @Bean
    public KafkaTemplate<String, DlqEvent> dlqKafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}
