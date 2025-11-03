package com.tguard.tguard_backend.user.service;

import com.tguard.tguard_backend.tenant.service.TenantService;
import com.tguard.tguard_backend.user.dto.AuthDtos;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TenantService tenantService;

    @Value("${tguard.auth.admin-code:}")
    private String adminSignupCode;

    public void signUp(AuthDtos.SignUpRequest req) {
        boolean isAdmin = isValidAdminCode(req.getAdminCode());
        String tenantId = resolveTenantId(req, isAdmin);

        if (!isAdmin) {
            tenantService.ensureActiveTenantOrThrow(tenantId);
        }

        if (userRepository.existsByUsernameAndTenantId(req.getUsername(), tenantId)) {
            throw new IllegalArgumentException("Username already exists for this tenant.");
        }
        if (userRepository.existsByPhoneNumberAndTenantId(req.getPhoneNumber(), tenantId)) {
            throw new IllegalArgumentException("Phone number already registered for this tenant.");
        }

        String role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";
        User user = User.builder()
                .tenantId(tenantId)
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .phoneNumber(req.getPhoneNumber())
                .role(role)
                .build();
        userRepository.save(user);
    }

    public AuthDtos.TokenResponse login(@Valid AuthDtos.LoginRequest req) {
        String tenantId = resolveTenantIdForLogin(req);
        User user = userRepository.findByUsernameAndTenantId(req.getUsername(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Username not found."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }

        String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole());
        return new AuthDtos.TokenResponse(token, "Bearer", user.getRole());
    }

    private boolean isValidAdminCode(String adminCode) {
        return StringUtils.hasText(adminCode) &&
                StringUtils.hasText(adminSignupCode) &&
                adminSignupCode.equals(adminCode.trim());
    }

    private String resolveTenantId(AuthDtos.SignUpRequest req, boolean isAdmin) {
        if (isAdmin) {
            return TenantService.GLOBAL_TENANT_ID;
        }
        if (!StringUtils.hasText(req.getTenantId())) {
            throw new IllegalArgumentException("tenantId is required for operator sign up.");
        }
        return req.getTenantId().trim();
    }

    private String resolveTenantIdForLogin(AuthDtos.LoginRequest req) {
        if (!StringUtils.hasText(req.getTenantId())) {
            throw new IllegalArgumentException("tenantId is required.");
        }
        String tenantId = req.getTenantId().trim();
        if (TenantService.GLOBAL_TENANT_ID.equals(tenantId)) {
            return TenantService.GLOBAL_TENANT_ID;
        }
        tenantService.ensureActiveTenantOrThrow(tenantId);
        return tenantId;
    }
}
