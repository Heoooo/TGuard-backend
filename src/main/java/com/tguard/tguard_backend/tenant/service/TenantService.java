package com.tguard.tguard_backend.tenant.service;

import com.tguard.tguard_backend.tenant.dto.TenantDtos;
import com.tguard.tguard_backend.tenant.entity.Tenant;
import com.tguard.tguard_backend.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {

    public static final String GLOBAL_TENANT_ID = "0";
    private static final String GLOBAL_TENANT_DISPLAY = "Global Administration";

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public boolean isActiveTenant(String tenantId) {
        String normalized = normalize(tenantId);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        if (GLOBAL_TENANT_ID.equals(normalized)) {
            return true;
        }
        return tenantRepository.existsByTenantIdAndActiveTrue(normalized);
    }

    @Transactional
    public TenantDtos.TenantResponse createTenant(TenantDtos.TenantCreateRequest request) {
        String normalized = normalize(request.tenantId());
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant id is required");
        }
        if (GLOBAL_TENANT_ID.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserved tenant id");
        }
        if (tenantRepository.existsByTenantId(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant id already exists");
        }
        Tenant saved = tenantRepository.save(Tenant.builder()
                .tenantId(normalized)
                .displayName(request.displayName().trim())
                .active(true)
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantDtos.TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantDtos.TenantValidationResponse validateTenant(String tenantId) {
        String normalized = normalize(tenantId);
        if (!StringUtils.hasText(normalized)) {
            return new TenantDtos.TenantValidationResponse(false, tenantId, null);
        }
        if (GLOBAL_TENANT_ID.equals(normalized)) {
            return new TenantDtos.TenantValidationResponse(true, GLOBAL_TENANT_ID, GLOBAL_TENANT_DISPLAY);
        }
        Tenant tenant = tenantRepository.findByTenantId(normalized)
                .filter(Tenant::isActive)
                .orElse(null);
        if (tenant == null) {
            return new TenantDtos.TenantValidationResponse(false, normalized, null);
        }
        return new TenantDtos.TenantValidationResponse(true, tenant.getTenantId(), tenant.getDisplayName());
    }

    @Transactional
    public void ensureActiveTenantOrThrow(String tenantId) {
        String normalized = normalize(tenantId);
        if (!StringUtils.hasText(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant id is required");
        }
        if (GLOBAL_TENANT_ID.equals(normalized)) {
            return;
        }
        if (!tenantRepository.existsByTenantIdAndActiveTrue(normalized)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant is not allowed");
        }
    }

    private String normalize(String tenantId) {
        if (tenantId == null) {
            return null;
        }
        return tenantId.trim();
    }

    private TenantDtos.TenantResponse toResponse(Tenant tenant) {
        return new TenantDtos.TenantResponse(
                tenant.getId(),
                tenant.getTenantId(),
                tenant.getDisplayName(),
                tenant.isActive()
        );
    }
}
