package com.groomthon.habiglow.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.groomthon.habiglow.domain.notification.dto.request.RegisterTokenRequest;
import com.groomthon.habiglow.domain.notification.entity.NotificationToken;
import com.groomthon.habiglow.domain.notification.enums.PushPlatform;
import com.groomthon.habiglow.domain.notification.repository.NotificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public static class SendResult {
        private final int success;
        private final int failure;
        public SendResult(int success, int failure) { this.success = success; this.failure = failure; }
        public int getSuccess() { return success; }
        public int getFailure() { return failure; }
    }

    @Transactional(readOnly = true)
    public SendResult sendToUser(Long userId, String title, String body, Map<String,String> data) throws Exception {
        List<String> tokens = tokenRepo.findByActiveTrue().stream()
                .filter(t -> Objects.equals(t.getUserId(), userId))
                .map(NotificationToken::getToken)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return sendToTokens(tokens, title, body, data);
    }

    @Transactional(readOnly = true)
    public SendResult sendBroadcast(String title, String body, Map<String,String> data) throws Exception {
        List<String> tokens = tokenRepo.findByActiveTrue().stream()
                .map(NotificationToken::getToken)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return sendToTokens(tokens, title, body, data);
    }

    private SendResult sendToTokens(List<String> tokenStrings, String title, String body, Map<String,String> data) throws Exception {
        if (tokenStrings.isEmpty()) return new SendResult(0,0);

        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);
        int success = 0, failure = 0;

        for (int i = 0; i < tokenStrings.size(); i += 500) {
            List<String> batch = tokenStrings.subList(i, Math.min(i+500, tokenStrings.size()));

            MulticastMessage.Builder builder = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build());

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            BatchResponse resp = messaging.sendMulticast(builder.build());
            success += resp.getSuccessCount();
            failure += resp.getFailureCount();

            // 무효 토큰 자동 비활성화
            for (int idx = 0; idx < resp.getResponses().size(); idx++) {
                var r = resp.getResponses().get(idx);
                if (!r.isSuccessful()) {
                    String bad = batch.get(idx);
                    String code = r.getException() != null && r.getException().getMessagingErrorCode() != null
                            ? r.getException().getMessagingErrorCode().name() : "UNKNOWN";
                    if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                        tokenRepo.findByToken(bad).ifPresent(t -> t.setActive(false));
                    }
                }
            }
        }
        return new SendResult(success, failure);
    }
}
