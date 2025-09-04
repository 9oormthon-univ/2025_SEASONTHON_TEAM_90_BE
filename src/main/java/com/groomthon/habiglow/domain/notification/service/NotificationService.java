package com.groomthon.habiglow.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.groomthon.habiglow.domain.notification.dto.request.RegisterTokenRequest;
import com.groomthon.habiglow.domain.notification.dto.response.SendResult;
import com.groomthon.habiglow.domain.notification.entity.NotificationToken;
import com.groomthon.habiglow.domain.notification.enums.PushPlatform;
import com.groomthon.habiglow.domain.notification.repository.NotificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTokenRepository tokenRepo;
    private final FirebaseApp firebaseApp;

    @Transactional
    public void registerToken(Long userId, RegisterTokenRequest req) {
        NotificationToken t = tokenRepo.findByUserIdAndDeviceId(userId, req.getDeviceId())
                .orElseGet(NotificationToken::new);
        t.setUserId(userId);
        t.setDeviceId(req.getDeviceId());
        t.setToken(req.getToken());
        t.setPlatform(PushPlatform.ANDROID); // 현재는 ANDROID 고정
        t.setActive(true);
        t.setLastSeenAt(LocalDateTime.now());
        tokenRepo.save(t);
    }

    @Transactional
    public void deactivateByToken(String token) {
        tokenRepo.findByToken(token).ifPresent(t -> {
            t.setActive(false);
            t.setLastSeenAt(LocalDateTime.now());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivateInvalidToken(String token, String errorCode) {
        try {
            tokenRepo.findByToken(token).ifPresent(t -> {
                t.setActive(false);
                t.setLastSeenAt(LocalDateTime.now());
            });
        } catch (Exception e) {
            // 로그는 남기되 메인 발송 프로세스에 영향을 주지 않도록 예외를 던지지 않음
            System.err.println("무효한 토큰 " + token + " 비활성화 실패 (오류코드: " + errorCode + "): " + e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public SendResult sendToUser(Long userId, String title, String body, Map<String,String> data) {
        List<String> tokens = tokenRepo.findByActiveTrue().stream()
                .filter(t -> Objects.equals(t.getUserId(), userId))
                .map(NotificationToken::getToken)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return sendToTokens(tokens, title, body, data);
    }

    @Transactional(readOnly = true)
    public SendResult sendBroadcast(String title, String body, Map<String,String> data) {
        List<String> tokens = tokenRepo.findByActiveTrue().stream()
                .map(NotificationToken::getToken)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return sendToTokens(tokens, title, body, data);
    }

    private SendResult sendToTokens(List<String> tokenStrings, String title, String body, Map<String,String> data) {
        if (tokenStrings.isEmpty()) return new SendResult(0,0);

        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
        int success = 0, failure = 0;

        for (int i = 0; i < tokenStrings.size(); i += 500) {
            List<String> batch = tokenStrings.subList(i, Math.min(i+500, tokenStrings.size()));

            try {
                MulticastMessage.Builder builder = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build());

                if (data != null && !data.isEmpty()) {
                    builder.putAllData(data);
                }

                BatchResponse resp = messaging.sendMulticast(builder.build());
                success += resp.getSuccessCount();
                failure += resp.getFailureCount();

                // 무효 토큰 자동 비활성화 (별도 트랜잭션에서 처리)
                processFailedResponses(batch, resp);

            } catch (FirebaseMessagingException e) {
                // Firebase 관련 예외 처리
                System.err.println("FCM 전송 중 오류 발생: " + e.getMessage() + " (에러코드: " + 
                    (e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "UNKNOWN") + ")");
                failure += batch.size(); // 전체 배치 실패로 처리
            } catch (Exception e) {
                // 기타 예외 처리
                System.err.println("알림 전송 중 예상치 못한 오류 발생: " + e.getMessage());
                failure += batch.size(); // 전체 배치 실패로 처리
            }
        }
        return new SendResult(success, failure);
    }

    private void processFailedResponses(List<String> batch, BatchResponse resp) {
        for (int idx = 0; idx < resp.getResponses().size(); idx++) {
            var response = resp.getResponses().get(idx);
            if (!response.isSuccessful()) {
                String invalidToken = batch.get(idx);
                String errorCode = response.getException() != null && response.getException().getMessagingErrorCode() != null
                        ? response.getException().getMessagingErrorCode().name() : "UNKNOWN";
                
                // 토큰 관련 오류의 경우 토큰 비활성화
                if (isTokenRelatedError(errorCode)) {
                    deactivateInvalidToken(invalidToken, errorCode);
                }
                
                // 디버깅을 위한 로그
                System.err.println("FCM 전송 실패 - 토큰: " + invalidToken + ", 오류코드: " + errorCode);
            }
        }
    }

    private boolean isTokenRelatedError(String errorCode) {
        return "UNREGISTERED".equals(errorCode) || 
               "INVALID_ARGUMENT".equals(errorCode) ||
               "NOT_FOUND".equals(errorCode);
    }
}
