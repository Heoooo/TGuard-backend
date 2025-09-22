package com.tguard.tguard_backend.card.service;

import com.tguard.tguard_backend.card.dto.request.CardRequest;
import com.tguard.tguard_backend.card.dto.response.CardResponse;
import com.tguard.tguard_backend.card.entity.Card;
import com.tguard.tguard_backend.card.repository.CardRepository;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public CardResponse registerCard(CardRequest request) {

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (cardRepository.existsByLast4AndUserId(request.last4(), request.userId())) {
            throw new RuntimeException("Card already registered");
        }

        Card saved = cardRepository.save(
                Card.builder()
                        .brand(request.brand())
                        .last4(request.last4())
                        .user(user)
                        .build()
        );

        return new CardResponse(saved.getId(), saved.getBrand(), saved.getLast4(), saved.getUser().getId());
    }
}
