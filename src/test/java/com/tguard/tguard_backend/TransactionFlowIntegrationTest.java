package com.tguard.tguard_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tguard.tguard_backend.kafka.config.KafkaConfig;
import com.tguard.tguard_backend.kafka.consumer.TransactionEventConsumer;
import com.tguard.tguard_backend.kafka.dto.TransactionEvent;
import com.tguard.tguard_backend.notification.sms.TwilioSmsSender;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.entity.RuleType;
import com.tguard.tguard_backend.rule.repository.RuleRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import com.tguard.tguard_backend.webhook.dto.PaymentWebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"webhook.payment.secret-key=test-secret"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {KafkaConfig.class, TransactionEventConsumer.class})
})
class TransactionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TwilioSmsSender twilioSmsSender;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Value("${webhook.payment.secret-key}")
    private String webhookSecretKey;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
        userRepository.deleteAll();

        User testUser = User.builder()
                .username("jinhyeok")
                .password("password")
                .phoneNumber("01012345678")
                .build();
        userRepository.save(testUser);

        Rule highAmountRule = Rule.builder()
                .ruleName("High Amount Rule")
                .description("300만원 초과 거래 탐지")
                .type(RuleType.AMOUNT)
                .active(true)
                .build();
        ruleRepository.save(highAmountRule);
    }

    @Test
    void testWebhookFlow() throws Exception {
        // 테스트 웹훅 payload
        PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent(
                "evt_test_12345", "APPROVED", "VISA", "1234",
                5_000_000L, "KRW", "Test Merchant", "ONLINE",
                LocalDateTime.now(), "pay_key_test", "U:jinhyeok:order123", null
        );

        String payload = objectMapper.writeValueAsString(webhookEvent);
        String signature = createSignature(payload);

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Payment-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        verify(twilioSmsSender).sendSms(any(String.class), contains("High Amount Rule"));
    }

    private String createSignature(String payload) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(webhookSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
