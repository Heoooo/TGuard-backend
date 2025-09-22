package com.tguard.tguard_backend.card.controller;

import com.tguard.tguard_backend.card.dto.request.CardRequest;
import com.tguard.tguard_backend.card.dto.response.CardResponse;
import com.tguard.tguard_backend.card.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> registerCard(@RequestBody CardRequest request) {
        CardResponse response = cardService.registerCard(request);
        return ResponseEntity.ok(response);
    }
}
