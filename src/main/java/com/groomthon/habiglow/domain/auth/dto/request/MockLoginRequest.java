package com.groomthon.habiglow.domain.auth.dto.request;

import com.groomthon.habiglow.global.oauth2.entity.SocialType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "개발용 Mock 로그인 요청 DTO")
public class MockLoginRequest {

	@Schema(description = "테스트용 이메일", example = "test@example.com")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	private String email;

	@Schema(description = "테스트용 사용자 이름", example = "테스트유저")
	@NotBlank(message = "사용자 이름은 필수입니다.")
	private String name;

	@Schema(description = "소셜 로그인 타입", example = "KAKAO")
	@Builder.Default
	private SocialType socialType = SocialType.KAKAO;

	@Schema(description = "테스트용 소셜 ID", example = "mock_user_001")
	@NotBlank(message = "소셜 ID는 필수입니다.")
	private String mockSocialId;
}