package com.tguard.tguard_backend.config;

import com.tguard.tguard_backend.kafka.dto.DlqEvent;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@Configuration
@Profile("test")
public class TestInfrastructureConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplateString() {
        RedisTemplate<String, String> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        return redisTemplate;
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
