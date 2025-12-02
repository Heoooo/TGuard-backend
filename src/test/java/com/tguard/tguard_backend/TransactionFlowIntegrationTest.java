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
import com.tguard.tguard_backend.webhook.service.PaymentWebhookService;
import com.tguard.tguard_backend.tenant.entity.Tenant;
import com.tguard.tguard_backend.tenant.repository.TenantRepository;
import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"webhook.secret=test-secret"})
@ActiveProfiles("test")
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {KafkaConfig.class, TransactionEventConsumer.class})
})
class TransactionFlowIntegrationTest {

    @MockBean
    private TwilioSmsSender twilioSmsSender;

    @MockBean
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentWebhookService paymentWebhookService;

    @Value("${webhook.secret}")
    private String webhookSecretKey;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId("tenant-a");

        tenantRepository.save(Tenant.builder()
                .tenantId("tenant-a")
                .displayName("Tenant A")
                .active(true)
                .build());

        User testUser = User.builder()
                .tenantId("tenant-a")
                .username("jinhyeok")
                .password("password")
                .phoneNumber("01012345678")
                .role("ROLE_USER")
                .build();
        userRepository.save(testUser);

        Rule highAmountRule = Rule.builder()
                .tenantId("tenant-a")
                .ruleName("High Amount Rule")
                .description("300만원 초과 거래 탐지")
                .type(RuleType.AMOUNT)
                .active(true)
                .build();
        ruleRepository.save(highAmountRule);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void testWebhookFlow() throws Exception {
        // 테스트 웹훅 payload
        PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent(
                "evt_test_12345", "APPROVED", "VISA", "1234",
                5_000_000L, "KRW", "Test Merchant", "ONLINE", "MOBILE",
                LocalDateTime.now(), "pay_key_test", "U:jinhyeok:order123", null, null);

        String payload = objectMapper.writeValueAsString(webhookEvent);
        String signature = createSignature(payload);

        paymentWebhookService.verifySignatureOrThrow(payload, signature);
        paymentWebhookService.handle(payload, null);

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    private String createSignature(String payload) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(webhookSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] digest = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(digest);
    }
}
