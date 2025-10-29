package com.tguard.tguard_backend.user.controller;


import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.user.dto.AuthDtos;
import com.tguard.tguard_backend.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Valid AuthDtos.SignUpRequest req) {
        authService.signUp(req);
        return ResponseEntity.ok(ApiResponse.success(null, "signup success"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.TokenResponse>> login(@RequestBody @Valid AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req)));
    }
}
