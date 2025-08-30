package com.groomthon.habiglow.domain.member.dto.request;

import java.util.List;

import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 관심사 수정 요청 DTO")
public class UpdateMemberInterestsRequest {
	
	@NotNull(message = "관심사 목록은 필수입니다.")
	@Schema(
		description = "관심사 루틴 카테고리 목록", 
		example = "[\"HEALTH\", \"LEARNING\", \"MINDFULNESS\"]"
	)
	private List<RoutineCategory> interests;
}