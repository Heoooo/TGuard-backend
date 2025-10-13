package com.tguard.tguard_backend.detection.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DetectionSummaryResponse(
        long totalAlerts,
        double averageRiskScore,
        double averageProbability,
        List<RuleCount> topRules,
        List<RecentAlert> recentAlerts
) {

    public record RuleCount(String ruleName, long count) { }

    public record RecentAlert(
            Long id,
            String ruleName,
            int riskScore,
            double probability,
            String summary,
            LocalDateTime detectedAt
    ) { }
}
