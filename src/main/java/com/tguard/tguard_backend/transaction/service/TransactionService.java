package com.tguard.tguard_backend.transaction.service;

import com.tguard.tguard_backend.detection.service.DetectionResultService;
import com.tguard.tguard_backend.transaction.dto.TransactionRequest;
import com.tguard.tguard_backend.transaction.dto.TransactionResponse;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.transaction.repository.TransactionRepository;
import com.tguard.tguard_backend.transaction.exception.TransactionNotFoundException;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.exception.UserNotFoundException;
import com.tguard.tguard_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DetectionResultService detectionResultService;

    /**
     * 사용자 거래를 생성하고, 이상 탐지 로직을 호출한다.
     */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        // 1. 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. 거래 객체 생성
        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.amount())
                .location(request.location())
                .deviceInfo(request.deviceInfo())
                .channel(request.channel())
                .transactionTime(LocalDateTime.now())
                .status(Transaction.Status.PENDING)
                .build();

        // 3. 거래 저장
        transactionRepository.save(transaction);

        // 4. 이상 거래 탐지 수행
        detectionResultService.analyzeAndSave(transaction);

        // 5. 응답 DTO로 반환
        return toResponse(transaction);
    }

    /**
     * 단일 거래 조회
     */
    @Transactional
    public TransactionResponse getTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(TransactionNotFoundException::new);
        return toResponse(transaction);
    }

    /**
     * 응답 DTO 매핑
     */
    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getUser().getId(),
                t.getAmount(),
                t.getLocation(),
                t.getDeviceInfo(),
                t.getTransactionTime(),
                t.getStatus().name()
        );
    }
}
