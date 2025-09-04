package com.groomthon.habiglow.domain.routine.dto.response;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "루틴 카테고리 응답 DTO")
public class RoutineCategoryResponse {
	
	@Schema(description = "카테고리 코드", example = "HEALTH")
	private String code;
	
	@Schema(description = "카테고리 설명", example = "건강")
	private String description;
	
	public static RoutineCategoryResponse from(RoutineCategory category) {
		return RoutineCategoryResponse.builder()
			.code(category.name())
			.description(category.getDescription())
			.build();
	}
	
	public static List<RoutineCategoryResponse> fromAll() {
		return Arrays.stream(RoutineCategory.values())
			.map(RoutineCategoryResponse::from)
			.collect(Collectors.toList());
	}
}