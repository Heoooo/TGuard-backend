package com.tguard.tguard_backend.transaction.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.transaction.dto.TransactionRequest;
import com.tguard.tguard_backend.transaction.dto.TransactionResponse;
import com.tguard.tguard_backend.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @RequestParam Long userId,
            @RequestBody @Valid TransactionRequest request
    ) {
        TransactionResponse result = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> get(@PathVariable Long id) {
        TransactionResponse result = transactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
