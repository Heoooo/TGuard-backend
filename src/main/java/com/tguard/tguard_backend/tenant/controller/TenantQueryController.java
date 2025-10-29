package com.tguard.tguard_backend.tenant.controller;

import com.tguard.tguard_backend.tenant.dto.TenantDtos;
import com.tguard.tguard_backend.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantQueryController {

    private final TenantService tenantService;

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDtos.TenantValidationResponse> validateTenant(@PathVariable String tenantId) {
        return ResponseEntity.ok(tenantService.validateTenant(tenantId));
    }
}
