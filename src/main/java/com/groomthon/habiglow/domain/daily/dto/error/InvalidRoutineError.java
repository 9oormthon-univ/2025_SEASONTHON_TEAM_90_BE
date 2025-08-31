package com.groomthon.habiglow.domain.daily.dto.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvalidRoutineError {
    
    private Long routineId;
    private String reason;
    
    public static InvalidRoutineError notFound(Long routineId) {
        return new InvalidRoutineError(routineId, "루틴을 찾을 수 없습니다");
    }
    
    public static InvalidRoutineError accessDenied(Long routineId) {
        return new InvalidRoutineError(routineId, "해당 루틴에 접근할 권한이 없습니다");
    }
}