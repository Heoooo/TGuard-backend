package com.tguard.tguard_backend.detection.service;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.detection.dto.DetectionResultResponse;
import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.detection.exception.DetectionResultNotFoundException;
import com.tguard.tguard_backend.detection.repository.DetectionResultRepository;
import com.tguard.tguard_backend.transaction.dto.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetectionResultQueryService {

    private final DetectionResultRepository detectionResultRepository;
    private final TransactionMapper transactionMapper;

    public List<DetectionResultResponse> getAllResults() {
        String tenantId = TenantContextHolder.requireTenantId();
        return detectionResultRepository.findTop10ByTenantIdOrderByDetectedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public DetectionResultResponse getResultById(Long id) {
        String tenantId = TenantContextHolder.requireTenantId();
        DetectionResult result = detectionResultRepository.findById(id)
                .filter(dr -> tenantId.equals(dr.getTenantId()))
                .orElseThrow(DetectionResultNotFoundException::new);
        return toResponse(result);
    }

    private DetectionResultResponse toResponse(DetectionResult result) {
        return new DetectionResultResponse(
                result.getId(),
                result.getTransaction().getId(),
                result.isSuspicious(),
                result.getReason(),
                result.getDetectedAt(),
                transactionMapper.toResponse(result.getTransaction())
        );
    }
}
