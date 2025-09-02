package com.groomthon.habiglow.domain.routine.dto.response;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 개별 루틴 감소 정보 응답
 */
@Getter
@Builder
public class ReductionReadyRoutineResponse {

	private Long routineId;
	private String title;
	private Integer currentTargetValue;
	private Integer suggestedTargetValue;
	private LocalDate lastAttemptDate;

	public static ReductionReadyRoutineResponse from(RoutineEntity routine, Integer suggestedTarget, LocalDate lastAttemptDate) {
		return ReductionReadyRoutineResponse.builder()
			.routineId(routine.getRoutineId())
			.title(routine.getTitle())
			.currentTargetValue(routine.getTargetValue())
			.suggestedTargetValue(suggestedTarget)
			.lastAttemptDate(lastAttemptDate)
			.build();
	}
}