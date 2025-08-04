package com.tguard.tguard_backend.user.service;

import com.tguard.tguard_backend.user.entity.User;
import com.tguard.tguard_backend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 앱 실행 시 기본 유저 1명 생성
    @PostConstruct
    public void initDefaultUser() {
        if (userRepository.count() == 0) {
            User defaultUser = User.builder()
                    .email("test@tguard.com")
                    .name("Test User")
                    .phoneNumber("010-0000-0000")
                    .build();
            userRepository.save(defaultUser);
        }
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}