package com.tguard.tguard_backend.tenant.repository;

import com.tguard.tguard_backend.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsByTenantId(String tenantId);
    boolean existsByTenantIdAndActiveTrue(String tenantId);
    Optional<Tenant> findByTenantId(String tenantId);
}
