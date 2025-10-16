package com.tguard.tguard_backend.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tguard.tguard_backend.card.dto.request.CardRequest;
import com.tguard.tguard_backend.card.repository.CardRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import com.tguard.tguard_backend.user.service.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .tenantId("tenant-a")
                .username("alice")
                .password("password")
                .phoneNumber("01011112222")
                .role("ROLE_USER")
                .build());
    }

    @Test
    @DisplayName("다른 테넌트 헤더로 접근 시 카드 조회가 차단된다")
    void cardLookupIsTenantIsolated() throws Exception {
        String token = jwtTokenProvider.createToken("alice", "ROLE_USER");
        CardRequest request = new CardRequest("VISA", "1234", "개인카드");

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk());

        assertThat(cardRepository.count()).isEqualTo(1);
        Long cardId = cardRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-b"))
                .andExpect(status().isNotFound());
    }
}
