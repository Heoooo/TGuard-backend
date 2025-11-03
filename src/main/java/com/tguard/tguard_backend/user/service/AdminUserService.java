package com.tguard.tguard_backend.user.service;

import com.tguard.tguard_backend.user.dto.UserAdminResponse;
import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserAdminResponse> getAdminUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserAdminResponse toResponse(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getTenantId(),
                user.getRole(),
                user.getPhoneNumber(),
                "ACTIVE",
                null
        );
    }
}
