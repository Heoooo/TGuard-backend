package com.tguard.tguard_backend.user.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.user.dto.UserAdminResponse;
import com.tguard.tguard_backend.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAdminResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getAdminUsers()));
    }
}
