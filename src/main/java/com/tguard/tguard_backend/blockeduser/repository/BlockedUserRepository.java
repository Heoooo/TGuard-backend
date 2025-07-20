package com.tguard.tguard_backend.blockeduser.repository;

import com.tguard.tguard_backend.blockeduser.entity.BlockedUser;
import com.tguard.tguard_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {
    List<BlockedUser> findByUser(User user);
}