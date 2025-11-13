package com.tguard.tguard_backend.webhook;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"default", "local", "dev", "test"})
public class WebhookIdempotencyStore {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public boolean markIfFirstSeen(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        return seen.add(key);
    }
}
