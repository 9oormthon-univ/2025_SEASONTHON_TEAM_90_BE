package com.groomthon.habiglow.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "SendTestRequest", description = "FCM 시연/테스트용 즉시 발송 요청 DTO")
public class SendTestRequest {

    @NotBlank
    @Schema(
            description = "푸시 알림 제목",
            example = "시연용 푸시 알림",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 100
    )
    private String title;

    @NotBlank
    @Schema(
            description = "푸시 알림 본문",
            example = "지금 푸시가 도착했어요.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 500
    )
    private String body;

    @Schema(
            description = "클라이언트에서 사용할 추가 데이터 (딥링크, 타입 등) - key/value 모두 문자열",
            example = "{\"route\":\"habiglow://routine/123\",\"type\":\"TEST\"}"
    )
    private Map<String, String> data;
}
