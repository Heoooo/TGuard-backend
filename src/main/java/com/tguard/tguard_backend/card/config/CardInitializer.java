package com.tguard.tguard_backend.card.config;

import com.tguard.tguard_backend.card.entity.Card;
import com.tguard.tguard_backend.card.repository.CardRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardInitializer {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void initTestCards() {
        try {
            if (cardRepository.count() == 0) {
                // id=1 가정하지 말고, 없으면 생성
                User user = userRepository.findById(1L).orElseGet(() ->
                        userRepository.save(User.builder()
                                .username("Test User")
                                .phoneNumber("010-0000-0000")
                                .build())
                );

                if (cardRepository.findByLast4("1234").isEmpty()) {
                    cardRepository.save(Card.builder().user(user).brand("VISA").last4("1234").build());
                }
                if (cardRepository.findByLast4("5678").isEmpty()) {
                    cardRepository.save(Card.builder().user(user).brand("MASTER").last4("5678").build());
                }
            }
        } catch (Exception e) {
            // 부팅 막지 않기
            // 로그만 남기고 조용히 종료
        }
    }
}
