package com.groomthon.habiglow.domain.routine.dto.response.adaptation;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 적응형 루틴 조정 대상 통합 조회 응답
 * 성장(증가)과 감소 대상을 함께 반환
 */
@Getter
@Builder
public class AdaptiveRoutineCheckResponse {

	private List<GrowthReadyRoutineResponse> growthReadyRoutines;     // 증가 대상
	private List<ReductionReadyRoutineResponse> reductionReadyRoutines; // 감소 대상
	private Integer totalGrowthReadyCount;
	private Integer totalReductionReadyCount;
	private Integer totalAdaptiveCount; // 전체 조정 대상 수

	public static AdaptiveRoutineCheckResponse of(
		List<GrowthReadyRoutineResponse> growthRoutines,
		List<ReductionReadyRoutineResponse> reductionRoutines) {

		return AdaptiveRoutineCheckResponse.builder()
			.growthReadyRoutines(growthRoutines)
			.reductionReadyRoutines(reductionRoutines)
			.totalGrowthReadyCount(growthRoutines.size())
			.totalReductionReadyCount(reductionRoutines.size())
			.totalAdaptiveCount(growthRoutines.size() + reductionRoutines.size())
			.build();
	}
}