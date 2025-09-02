package com.groomthon.habiglow.domain.routine.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 난이도 감소 대상 루틴 조회 응답
 */
@Getter
@Builder
public class DifficultyReductionCheckResponse {

	private List<ReductionReadyRoutineResponse> reductionReadyRoutines;
	private Integer totalReductionReadyCount;

	public static DifficultyReductionCheckResponse of(List<ReductionReadyRoutineResponse> routines) {
		return DifficultyReductionCheckResponse.builder()
			.reductionReadyRoutines(routines)
			.totalReductionReadyCount(routines.size())
			.build();
	}
}