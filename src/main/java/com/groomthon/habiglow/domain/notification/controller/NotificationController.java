package com.groomthon.habiglow.domain.notification.controller;

import com.groomthon.habiglow.domain.notification.dto.request.RegisterTokenRequest;
import com.groomthon.habiglow.domain.notification.dto.request.SendTestRequest;
import com.groomthon.habiglow.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "알림 관리 API", description = "FCM 푸시 토큰 등록/비활성화 및 테스트 발송")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "FCM 토큰 등록/갱신")
    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @Valid @RequestBody RegisterTokenRequest req
    ) {
        notificationService.registerToken(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "FCM 토큰 비활성화")
    @DeleteMapping("/tokens/{token}")
    public ResponseEntity<Void> deactivate(@PathVariable String token) {
        notificationService.deactivateByToken(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "본인 기기 테스트 발송")
    @PostMapping("/send-test")
    public ResponseEntity<Map<String,Object>> sendTest(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @Valid @RequestBody SendTestRequest req
    ) throws Exception {
        NotificationService.SendResult r =
                notificationService.sendToUser(userId, req.getTitle(), req.getBody(), req.getData());
        return ResponseEntity.ok(Map.of("success", r.getSuccess(), "failure", r.getFailure()));
    }
}
