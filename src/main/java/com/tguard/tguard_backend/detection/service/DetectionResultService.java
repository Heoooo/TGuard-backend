package com.tguard.tguard_backend.detection.service;

import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.detection.repository.DetectionResultRepository;
import com.tguard.tguard_backend.rule.service.RuleService;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DetectionResultService {

    private final DetectionResultRepository detectionResultRepository;
    private final RuleService ruleService;

    /**
     * 거래에 대해 이상 여부를 분석하고 결과를 저장
     */
    @Transactional
    public void analyzeAndSave(Transaction tx) {
        boolean isSuspicious = ruleService.isSuspicious(tx);

        DetectionResult result = DetectionResult.builder()
                .transaction(tx)
                .isSuspicious(isSuspicious)
                .reason(isSuspicious ? "룰 기반 이상 거래 감지" : null)
                .detectedAt(LocalDateTime.now())
                .build();

        detectionResultRepository.save(result);
        tx.updateStatus(isSuspicious ? Transaction.Status.REJECTED : Transaction.Status.APPROVED);
    }

    /**
     * 탐지 알고리즘: 고액 거래 여부
     */
    private boolean isAbnormal(Transaction t) {
        return t.getAmount() != null && t.getAmount() > 3_000_000;
    }
}