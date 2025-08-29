package com.groomthon.habiglow.global.oauth2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "소셜 로그인 성공 응답")
public class OAuthLoginResponse {

	@Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
	private final String accessToken;

	@Schema(description = "회원 이메일", example = "test@example.com")
	private final String email;

	@Schema(description = "회원 이름", example = "테스트 유저")
	private final String name;

	@Schema(description = "소셜 타입", example = "GOOGLE")
	private final String socialType;
}