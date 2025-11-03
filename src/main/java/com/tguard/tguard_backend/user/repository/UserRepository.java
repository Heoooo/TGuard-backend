package com.tguard.tguard_backend.user.repository;

import com.tguard.tguard_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsernameAndTenantId(String username, String tenantId);
    boolean existsByPhoneNumberAndTenantId(String phoneNumber, String tenantId);
    Optional<User> findByUsernameAndTenantId(String username, String tenantId);
    Optional<User> findByIdAndTenantId(Long id, String tenantId);
    Optional<User> findByPhoneNumberAndTenantId(String phoneNumber, String tenantId);
    List<User> findByTenantIdAndPhoneNumberIn(String tenantId, Collection<String> phoneNumbers);
}
