package com.lifelink.repository;

import com.lifelink.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndDedupeKeyAndReadFalse(Long userId, String dedupeKey);

    List<Notification> findByUserIdAndReadFalse(Long userId);
}
