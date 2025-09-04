package com.groomthon.habiglow.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Schema(description = "푸시 알림 토큰 등록 요청 DTO")
public class RegisterTokenRequest {

    @NotBlank
    @Schema(description = "FCM(Firebase Cloud Messaging) 기기 토큰", example = "fcm_token_example_12345")
    private String token;

    @NotBlank
    @Schema(description = "사용자의 기기를 식별하는 고유 ID", example = "device_id_example_67890")
    private String deviceId;
}