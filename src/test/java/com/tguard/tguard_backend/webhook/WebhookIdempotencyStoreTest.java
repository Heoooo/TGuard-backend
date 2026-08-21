package com.tguard.tguard_backend.webhook;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookIdempotencyStoreTest {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private WebhookIdempotencyStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store = new WebhookIdempotencyStore(redisTemplate);
        ReflectionTestUtils.setField(store, "ttlSeconds", 60L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void storesIdempotencyKeyWithTtlWhenFirstSeen() {
        TenantContextHolder.setTenantId("tenant-a");
        when(valueOperations.setIfAbsent("tenant:tenant-a:webhook:idempotency:evt-1", "1", Duration.ofSeconds(60)))
                .thenReturn(true);

        boolean firstSeen = store.markIfFirstSeen(" evt-1 ");

        assertThat(firstSeen).isTrue();
    }

    @Test
    void returnsFalseWhenKeyAlreadyExists() {
        TenantContextHolder.setTenantId("tenant-a");
        when(valueOperations.setIfAbsent("tenant:tenant-a:webhook:idempotency:evt-1", "1", Duration.ofSeconds(60)))
                .thenReturn(false);

        boolean firstSeen = store.markIfFirstSeen("evt-1");

        assertThat(firstSeen).isFalse();
    }

    @Test
    void skipsRedisWhenKeyIsBlank() {
        boolean firstSeen = store.markIfFirstSeen(" ");

        assertThat(firstSeen).isTrue();
        verify(valueOperations, never()).setIfAbsent(any(), any(), any(Duration.class));
    }

    @Test
    void failsClosedWhenRedisCannotCheckKey() {
        TenantContextHolder.setTenantId("tenant-a");
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> store.markIfFirstSeen("evt-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to verify webhook idempotency key");
    }
}
