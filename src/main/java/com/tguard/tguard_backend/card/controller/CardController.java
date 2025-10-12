package com.tguard.tguard_backend.card.controller;

import com.tguard.tguard_backend.card.dto.request.CardRequest;
import com.tguard.tguard_backend.card.dto.response.CardResponse;
import com.tguard.tguard_backend.card.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> registerCard(
            @AuthenticationPrincipal String username,
            @RequestBody @Valid CardRequest request) {
        return ResponseEntity.ok(cardService.register(username, request));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getMyCards(
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(cardService.findMyCards(username));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getMyCard(
            @AuthenticationPrincipal String username,
            @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.findMyCard(username, cardId));
    }

    @PatchMapping("/{cardId}/nickname")
    public ResponseEntity<CardResponse> updateNickname(
            @AuthenticationPrincipal String username,
            @PathVariable Long cardId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cardService.rename(username, cardId, body.get("nickname")));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteMyCard(
            @AuthenticationPrincipal String username,
            @PathVariable Long cardId) {
        cardService.delete(username, cardId);
        return ResponseEntity.noContent().build();
    }
}
