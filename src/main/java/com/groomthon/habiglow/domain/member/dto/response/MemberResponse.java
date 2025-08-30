package com.groomthon.habiglow.domain.member.dto.response;

import java.util.List;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineCategoryResponse;
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

	@Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
	private String profileImageUrl;

	@Schema(description = "관심사 루틴 카테고리 목록")
	private List<RoutineCategoryResponse> interests;


	public static MemberResponse fromEntity(MemberEntity memberEntity) {
		List<RoutineCategoryResponse> interestResponses = memberEntity.getInterestCategories()
			.stream()
			.map(RoutineCategoryResponse::from)
			.toList();
			
		return MemberResponse.builder()
			.id(memberEntity.getId())
			.memberName(memberEntity.getMemberName())
			.memberEmail(memberEntity.getMemberEmail())
			.socialType(memberEntity.getSocialType())
			.profileImageUrl(memberEntity.getProfileImageUrl())
			.interests(interestResponses)
			.build();
	}
}