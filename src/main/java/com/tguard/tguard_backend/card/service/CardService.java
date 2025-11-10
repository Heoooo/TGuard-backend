package com.tguard.tguard_backend.card.service;

import com.tguard.tguard_backend.card.dto.request.CardRequest;
import com.tguard.tguard_backend.card.dto.response.CardResponse;
import com.tguard.tguard_backend.card.entity.Card;
import com.tguard.tguard_backend.card.repository.CardRepository;
import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public CardResponse register(String username, CardRequest req) {
        String tenantId = TenantContextHolder.requireTenantId();
        User owner = userRepository.findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found."));

        if (cardRepository.existsByOwner_TenantIdAndOwner_UsernameAndBrandIgnoreCaseAndLast4(tenantId, username, req.brand(), req.last4())) {
            throw new ResponseStatusException(CONFLICT, "Card already registered.");
        }

        String nickname = req.nickname();
        if (nickname != null) {
            nickname = nickname.trim();
        }

        Card saved = cardRepository.save(
                Card.builder()
                        .owner(owner)
                        .brand(req.brand().trim())
                        .last4(req.last4().trim())
                        .nickname(nickname)
                        .build()
        );
        return toResponse(saved);
    }

    public List<CardResponse> findMyCards(String username) {
        String tenantId = TenantContextHolder.requireTenantId();
        return cardRepository.findByOwner_TenantIdAndOwner_Username(tenantId, username)
                .stream().map(this::toResponse).toList();
    }

    public CardResponse findMyCard(String username, Long cardId) {
        String tenantId = TenantContextHolder.requireTenantId();
        Card card = cardRepository.findByIdAndOwner_TenantIdAndOwner_Username(cardId, tenantId, username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Card not found."));
        return toResponse(card);
    }

    @Transactional
    public CardResponse rename(String username, Long cardId, String nickname) {
        String tenantId = TenantContextHolder.requireTenantId();
        Card card = cardRepository.findByIdAndOwner_TenantIdAndOwner_Username(cardId, tenantId, username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Card not found."));
        if (nickname != null) {
            card.setNickname(nickname.trim());
        } else {
            card.setNickname(null);
        }
        return toResponse(card);
    }

    @Transactional
    public void delete(String username, Long cardId) {
        String tenantId = TenantContextHolder.requireTenantId();
        Card card = cardRepository.findByIdAndOwner_TenantIdAndOwner_Username(cardId, tenantId, username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Card not found."));
        cardRepository.delete(card);
    }

    private CardResponse toResponse(Card c) {
        return new CardResponse(c.getId(), c.getBrand(), c.getLast4(), c.getNickname());
    }
}
