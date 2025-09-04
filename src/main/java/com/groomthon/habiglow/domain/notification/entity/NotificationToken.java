package com.groomthon.habiglow.domain.notification.entity;

import com.groomthon.habiglow.domain.notification.enums.PushPlatform;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private PushPlatform platform = PushPlatform.ANDROID; // 지금은 ANDROID 고정

    @Column(name="device_id", nullable=false, length=128)
    private String deviceId;

    private LocalDateTime lastSeenAt;

    @Column(name="is_active", nullable=false)
    private boolean active = true;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
