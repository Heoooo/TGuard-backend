package com.tguard.tguard_backend.detection.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.detection.dto.DetectionResultResponse;
import com.tguard.tguard_backend.detection.service.DetectionResultQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/detections")
@RequiredArgsConstructor
public class DetectionResultController {

    private final DetectionResultQueryService detectionResultQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DetectionResultResponse>>> getAll() {
        List<DetectionResultResponse> list = detectionResultQueryService.getAllResults();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DetectionResultResponse>> getOne(@PathVariable Long id) {
        DetectionResultResponse result = detectionResultQueryService.getResultById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
