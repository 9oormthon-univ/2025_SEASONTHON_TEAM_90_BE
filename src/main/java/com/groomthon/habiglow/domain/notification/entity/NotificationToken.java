package com.groomthon.habiglow.domain.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_tokens",
        uniqueConstraints = @UniqueConstraint(name="uq_user_device", columnNames={"user_id","device_id"}),
        indexes = {
                @Index(name="idx_token_active", columnList="token,is_active"),
                @Index(name="idx_user_active",  columnList="user_id,is_active")
        }
)
@Getter @Setter
public class NotificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(nullable=false, length=2048)
    private String token;

    @Column(name="device_id", nullable=false, length=128)
    private String deviceId;

    private LocalDateTime lastSeenAt;

    @Column(name="is_active", nullable=false)
    private boolean active = true;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable=false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void onUpdate(){ this.updatedAt = LocalDateTime.now(); }
}
