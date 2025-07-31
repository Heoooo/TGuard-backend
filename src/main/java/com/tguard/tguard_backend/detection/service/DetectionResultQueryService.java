package com.tguard.tguard_backend.detection.service;

import com.tguard.tguard_backend.detection.dto.DetectionResultResponse;
import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.detection.repository.DetectionResultRepository;
import com.tguard.tguard_backend.detection.exception.DetectionResultNotFoundException;
import com.tguard.tguard_backend.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetectionResultQueryService {

    private final DetectionResultRepository detectionResultRepository;
    private final TransactionService transactionService;

    public List<DetectionResultResponse> getAllResults() {
        return detectionResultRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public DetectionResultResponse getResultById(Long id) {
        DetectionResult result = detectionResultRepository.findById(id)
                .orElseThrow(DetectionResultNotFoundException::new);
        return toResponse(result);
    }

    private DetectionResultResponse toResponse(DetectionResult result) {
        return new DetectionResultResponse(
                result.getId(),
                result.getTransaction().getId(),
                result.getIsSuspicious(),
                result.getReason(),
                result.getDetectedAt(),
                transactionService.toResponse(result.getTransaction())
        );
    }
}
