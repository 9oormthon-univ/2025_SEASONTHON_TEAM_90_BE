package com.groomthon.habiglow.domain.member.dto.response;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 정보 응답 DTO")
public class MemberResponse {

	@Schema(description = "회원 고유 ID", example = "1")
	private Long id;

	@Schema(description = "회원 이름", example = "홍길동")
	private String memberName;

	@Schema(description = "회원 이메일", example = "hong@example.com")
	private String memberEmail;

	@Schema(description = "회원 소셜 타입", example = "GOOGLE")
	private SocialType socialType;


	public static MemberResponse fromEntity(MemberEntity memberEntity) {
		return MemberResponse.builder()
			.id(memberEntity.getId())
			.memberName(memberEntity.getMemberName())
			.memberEmail(memberEntity.getMemberEmail())
			.socialType(memberEntity.getSocialType())
			.build();
	}
}