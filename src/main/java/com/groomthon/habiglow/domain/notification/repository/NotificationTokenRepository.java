package com.groomthon.habiglow.domain.notification.repository;

import com.groomthon.habiglow.domain.notification.entity.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {
    
    //userId와 deviceId를 사용하여 알림 토큰 조회
    Optional<NotificationToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    //토큰 유효성 확인 & 이미 데이터베이스에 존재하는지 확인
    Optional<NotificationToken> findByToken(String token);

    // active 상태가 true인 모든 알림 토큰 목록 조회
    List<NotificationToken> findByActiveTrue();
}
