package com.groomthon.habiglow.domain.member.dto.response;

import java.util.List;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineCategoryResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 관심사 조회 응답 DTO")
public class MemberInterestsResponse {
	
	@Schema(description = "회원 고유 ID", example = "1")
	private Long memberId;
	
	@Schema(description = "관심사 루틴 카테고리 목록")
	private List<RoutineCategoryResponse> interests;
	
	public static MemberInterestsResponse from(MemberEntity member) {
		List<RoutineCategoryResponse> interestResponses = member.getInterestCategories()
			.stream()
			.map(RoutineCategoryResponse::from)
			.toList();
			
		return MemberInterestsResponse.builder()
			.memberId(member.getId())
			.interests(interestResponses)
			.build();
	}
}