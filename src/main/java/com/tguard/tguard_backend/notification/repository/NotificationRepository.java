package com.tguard.tguard_backend.notification.repository;

import com.tguard.tguard_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTenantIdAndTransaction_User_IdOrderByCreatedAtDesc(String tenantId, Long userId);
}
