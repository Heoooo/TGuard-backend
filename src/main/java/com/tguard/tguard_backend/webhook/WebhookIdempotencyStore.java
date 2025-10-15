package com.tguard.tguard_backend.webhook;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"default","local","dev","test"})
public class WebhookIdempotencyStore {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    /** 최초면 true, 이미 처리됨은 false */
    public boolean markIfFirstSeen(String key) {
        if (key == null || key.isBlank()) return true; // 키가 없으면 우회(이벤트ID로 보강)
        return seen.add(key);
    }
}
