package com.groomthon.habiglow.domain.routine.dto.response;

import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

/**
 * @deprecated 이 클래스는 제거됩니다. RoutineAdaptationResultResponse를 사용하세요.
 */
@Deprecated
public class IncreaseTargetResponse extends RoutineAdaptationResultResponse {
    
    public static RoutineAdaptationResultResponse from(RoutineEntity routine, Integer previousTarget) {
        return RoutineAdaptationResultResponse.success(
            routine.getRoutineId(),
            routine.getTitle(),
            previousTarget,
            routine.getTargetValue(),
            AdaptationAction.INCREASE
        );
    }
}