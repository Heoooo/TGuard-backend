package com.tguard.tguard_backend.webhook;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WebhookIdempotencyStore {

    private static final String KEY_PREFIX = "webhook:idempotency:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${tguard.webhook.idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    public boolean markIfFirstSeen(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        try {
            Boolean inserted = redisTemplate.opsForValue()
                    .setIfAbsent(namespacedKey(key), "1", Duration.ofSeconds(resolveTtlSeconds()));
            return Boolean.TRUE.equals(inserted);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to verify webhook idempotency key", e);
        }
    }

    private String namespacedKey(String key) {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            tenantId = "global";
        }
        return "tenant:" + tenantId.trim() + ":" + KEY_PREFIX + key.trim();
    }

    private long resolveTtlSeconds() {
        return Math.max(1, ttlSeconds);
    }
}
