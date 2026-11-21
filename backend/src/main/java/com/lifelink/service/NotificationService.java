package com.lifelink.service;

import com.lifelink.model.Notification;
import com.lifelink.model.NotificationType;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.NotificationRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** In-app notifications only. Nothing is sent by e-mail or SMS. */
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationService(NotificationRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    @Transactional
    public void notify(User user, NotificationType type, String message, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message.length() > 300 ? message.substring(0, 300) : message);
        n.setLink(link);
        repo.save(n);
    }

    /** Notifies every admin and staff member. If dedupeKey is given, skips users who already have it unread. */
    @Transactional
    public void notifyStaff(NotificationType type, String message, String link, String dedupeKey) {
        for (User u : users.findByRoleIn(List.of(Role.ADMIN, Role.STAFF))) {
            if (!u.isEnabled()) {
                continue;
            }
            if (dedupeKey != null && repo.existsByUserIdAndDedupeKeyAndReadFalse(u.getId(), dedupeKey)) {
                continue;
            }
            Notification n = new Notification();
            n.setUser(u);
            n.setType(type);
            n.setMessage(message.length() > 300 ? message.substring(0, 300) : message);
            n.setLink(link);
            n.setDedupeKey(dedupeKey);
            repo.save(n);
        }
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> latest(Long userId) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = repo.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        repo.saveAll(unread);
    }
}
