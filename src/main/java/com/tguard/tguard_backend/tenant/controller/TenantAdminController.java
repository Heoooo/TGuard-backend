package com.tguard.tguard_backend.tenant.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.tenant.dto.TenantDtos;
import com.tguard.tguard_backend.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantDtos.TenantResponse>>> getTenants() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getAllTenants()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantDtos.TenantResponse>> createTenant(
            @RequestBody @Valid TenantDtos.TenantCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.createTenant(request)));
    }
}
