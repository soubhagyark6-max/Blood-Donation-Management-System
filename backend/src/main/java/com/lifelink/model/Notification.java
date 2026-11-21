package com.lifelink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** In-app notification only. No e-mail or SMS is sent by this system. */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_user", columnList = "user_id,read_flag"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(length = 200)
    private String link;

    @Column(name = "read_flag", nullable = false)
    private boolean read = false;

    /** Used to avoid creating the same alert again and again (e.g. "LOW_STOCK:A_POS:PLATELETS"). */
    @Column(length = 100)
    private String dedupeKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
