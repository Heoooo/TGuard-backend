package com.tguard.tguard_backend.detection.service;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.detection.entity.DetectionResult;
import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.detection.repository.DetectionResultRepository;
import com.tguard.tguard_backend.detection.service.dto.DetectionSummaryResponse;
import com.tguard.tguard_backend.detection.service.dto.DetectionSummaryResponse.RecentAlert;
import com.tguard.tguard_backend.detection.service.dto.DetectionSummaryResponse.RuleCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetectionAnalyticsService {

    private final DetectionResultRepository detectionResultRepository;

    public DetectionSummaryResponse getSummary() {
        String tenantId = TenantContextHolder.requireTenantId();
        long totalAlerts = detectionResultRepository.countByTenantId(tenantId);
        double avgScore = detectionResultRepository.averageRiskScore(tenantId).orElse(0.0);
        double avgProbability = detectionResultRepository.averageProbability(tenantId).orElse(0.0);

        List<RuleCount> topRules = detectionResultRepository.countByRule(tenantId, PageRequest.of(0, 3)).stream()
                .map(tuple -> new RuleCount((String) tuple[0], ((Number) tuple[1]).longValue()))
                .collect(Collectors.toList());

        List<RecentAlert> recentAlerts = detectionResultRepository.findTop10ByTenantIdOrderByDetectedAtDesc(tenantId).stream()
                .map(this::toRecentAlert)
                .collect(Collectors.toList());

        return new DetectionSummaryResponse(
                totalAlerts,
                avgScore,
                avgProbability,
                topRules,
                recentAlerts
        );
    }

    private RecentAlert toRecentAlert(DetectionResult result) {
        return new RecentAlert(
                result.getId(),
                Optional.ofNullable(result.getRule()).map(Rule::getRuleName).orElse("unknown"),
                result.getRiskScore(),
                result.getProbability(),
                result.getReason(),
                result.getDetectedAt()
        );
    }
}
