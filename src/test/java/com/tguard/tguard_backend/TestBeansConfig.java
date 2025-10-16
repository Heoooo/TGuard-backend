package com.tguard.tguard_backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import static org.mockito.Mockito.*;

@TestConfiguration
public class TestBeansConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplate() {
        // 실제 Redis 연결 없이 테스트할 수 있도록 Mock RedisTemplate을 제공
        return mock(RedisTemplate.class);
    }
}
