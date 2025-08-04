package com.tguard.tguard_backend.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SlackNotifier {

    @Value("${slack.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String message) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("text", message);
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("Slack 알림 전송 완료: {}", message);
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
        }
    }
}