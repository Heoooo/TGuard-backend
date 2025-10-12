package com.tguard.tguard_backend.card.config;

import com.tguard.tguard_backend.card.entity.Card;
import com.tguard.tguard_backend.card.repository.CardRepository;
import com.tguard.tguard_backend.common.tenant.TenantProperties;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class CardInitializer {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final TenantProperties tenantProperties;

    @PostConstruct
    public void initTestCards() {
        try {
            String tenantId = tenantProperties.defaultTenantOr("default");
            User user = userRepository.findByUsernameAndTenantId("testuser", tenantId).orElseGet(() ->
                    userRepository.save(User.builder()
                            .tenantId(tenantId)
                            .username("testuser")
                            .password("$2a$10$dummyhashedpasswordforlocalinit")
                            .phoneNumber("010-0000-0000")
                            .role("ROLE_USER")
                            .build())
            );

            if (cardRepository.findByOwner_TenantIdAndLast4(tenantId, "1234").isEmpty()) {
                cardRepository.save(Card.builder().owner(user).brand("VISA").last4("1234").build());
            }
            if (cardRepository.findByOwner_TenantIdAndLast4(tenantId, "5678").isEmpty()) {
                cardRepository.save(Card.builder().owner(user).brand("MASTER").last4("5678").build());
            }
        } catch (Exception ignored) {
        }
    }
}
