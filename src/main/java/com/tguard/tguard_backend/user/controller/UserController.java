package com.tguard.tguard_backend.user.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.common.tenant.TenantContextHolder;
import com.tguard.tguard_backend.user.dto.UserProfileResponse;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (authentication != null) {
            username = authentication.getName();
        }
        String tenantId = TenantContextHolder.requireTenantId();

        User user = userRepository.findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
