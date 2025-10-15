package com.tguard.tguard_backend.user.service;

import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.user.dto.AuthDtos;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signUp(AuthDtos.SignUpRequest req) {
        String tenantId = TenantContextHolder.requireTenantId();
        if (userRepository.existsByUsernameAndTenantId(req.getUsername(), tenantId)) {
            throw new IllegalArgumentException("Username already exists for this tenant.");
        }
        if (userRepository.existsByPhoneNumberAndTenantId(req.getPhoneNumber(), tenantId)) {
            throw new IllegalArgumentException("Phone number already registered for this tenant.");
        }
        User user = User.builder()
                .tenantId(tenantId)
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .phoneNumber(req.getPhoneNumber())
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
    }

    public AuthDtos.TokenResponse login(@Valid AuthDtos.LoginRequest req) {
        String tenantId = TenantContextHolder.requireTenantId();
        User user = userRepository.findByUsernameAndTenantId(req.getUsername(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Username not found."));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }
        String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole());
        return new AuthDtos.TokenResponse(token, "Bearer");
    }
}
