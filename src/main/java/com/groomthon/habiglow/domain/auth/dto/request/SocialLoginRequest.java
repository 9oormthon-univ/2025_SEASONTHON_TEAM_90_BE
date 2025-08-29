package com.groomthon.habiglow.domain.auth.dto.request;

import com.groomthon.habiglow.global.oauth2.entity.SocialType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "클라이언트 소셜 로그인 요청")
public class SocialLoginRequest {
    
    @NotBlank(message = "소셜 액세스 토큰은 필수입니다")
    @Schema(description = "소셜 플랫폼에서 발급받은 액세스 토큰", example = "ya29.a0AfH6SMC...")
    private String socialAccessToken;
    
    @NotNull(message = "소셜 타입은 필수입니다")
    @Schema(description = "소셜 로그인 타입", example = "GOOGLE")
    private SocialType socialType;
}