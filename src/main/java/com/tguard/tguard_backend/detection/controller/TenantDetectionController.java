package com.tguard.tguard_backend.detection.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.detection.dto.DetectionResultResponse;
import com.tguard.tguard_backend.detection.service.DetectionAnalyticsService;
import com.tguard.tguard_backend.detection.service.DetectionResultQueryService;
import com.tguard.tguard_backend.detection.service.dto.DetectionSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tenant scoped detection APIs that can be used by both administrators and operators.
 * Data access is still restricted per tenant via {@link com.tguard.tguard_backend.common.tenant.TenantContextHolder}.
 */
@RestController
@RequestMapping("/api/detections")
@RequiredArgsConstructor
public class TenantDetectionController {

    private final DetectionResultQueryService detectionResultQueryService;
    private final DetectionAnalyticsService detectionAnalyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DetectionResultResponse>>> listDetections() {
        return ResponseEntity.ok(ApiResponse.success(detectionResultQueryService.getAllResults()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DetectionSummaryResponse>> detectionSummary() {
        return ResponseEntity.ok(ApiResponse.success(detectionAnalyticsService.getSummary()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DetectionResultResponse>> detectionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(detectionResultQueryService.getResultById(id)));
    }
}
