package com.groomthon.habiglow.domain.routine.dto.response;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

import lombok.Builder;
import lombok.Getter;

/**
 * 목표값 감소 실행 응답
 */
@Getter
@Builder
public class DecreaseTargetResponse {

	private Long routineId;
	private Integer previousTargetValue;
	private Integer newTargetValue;
	private Integer decreaseAmount;
	private LocalDate updatedDate;

	public static DecreaseTargetResponse from(RoutineEntity routine, Integer previousTarget) {
		Integer decreaseAmount = previousTarget - routine.getTargetValue();

		return DecreaseTargetResponse.builder()
			.routineId(routine.getRoutineId())
			.previousTargetValue(previousTarget)
			.newTargetValue(routine.getTargetValue())
			.decreaseAmount(decreaseAmount)
			.updatedDate(LocalDate.now())
			.build();
	}
}