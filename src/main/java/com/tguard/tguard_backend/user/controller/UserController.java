package com.tguard.tguard_backend.user.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable Long id) {
        UserProfileResponse profile = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // 보안은 나중에 처리 예정 (로그인/회원가입 등)
}