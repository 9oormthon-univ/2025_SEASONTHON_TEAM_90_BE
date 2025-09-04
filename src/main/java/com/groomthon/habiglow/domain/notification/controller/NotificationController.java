package com.groomthon.habiglow.domain.notification.controller;

import com.groomthon.habiglow.domain.notification.dto.request.RegisterTokenRequest;
import com.groomthon.habiglow.domain.notification.dto.request.SendTestRequest;
import com.groomthon.habiglow.domain.notification.dto.response.SendResult;
import com.groomthon.habiglow.domain.notification.service.NotificationService;
import com.groomthon.habiglow.global.response.ApiSuccessCode;
import com.groomthon.habiglow.global.response.AutoApiResponse;
import com.groomthon.habiglow.global.response.SuccessCode;
import com.groomthon.habiglow.global.swagger.CustomExceptionDescription;
import com.groomthon.habiglow.global.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@AutoApiResponse
@Tag(name = "알림 관리 API", description = "FCM 푸시 토큰 등록/비활성화 및 테스트 발송")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "FCM 토큰 등록/갱신",
        description = "사용자의 FCM 토큰을 등록하거나 갱신합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/tokens")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public void registerToken(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @Valid @RequestBody RegisterTokenRequest req
    ) {
        notificationService.registerToken(userId, req);
    }

    @Operation(
        summary = "FCM 토큰 비활성화",
        description = "지정된 토큰을 비활성화합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 비활성화 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 토큰 형식")
    })
    @DeleteMapping("/tokens/{token}")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public void deactivate(@PathVariable String token) {
        notificationService.deactivateByToken(token);
    }

    @Operation(
        summary = "본인 기기 테스트 발송",
        description = "로그인한 사용자의 기기로 테스트 푸시 알림을 발송합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "테스트 발송 완료"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/send-test")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
    @SuccessCode(ApiSuccessCode.SUCCESS)
    public Map<String,Object> sendTest(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @Valid @RequestBody SendTestRequest req
    ) {
        SendResult r = notificationService.sendToUser(userId, req.getTitle(), req.getBody(), req.getData());
        return Map.of("success", r.getSuccess(), "failure", r.getFailure());
    }
}
