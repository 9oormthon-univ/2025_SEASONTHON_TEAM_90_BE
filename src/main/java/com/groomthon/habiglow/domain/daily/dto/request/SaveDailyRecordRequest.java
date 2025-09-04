package com.groomthon.habiglow.domain.daily.dto.request;

import java.util.List;

import com.groomthon.habiglow.domain.daily.entity.EmotionType;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SaveDailyRecordRequest {
    
    @Valid
    private ReflectionRequest reflection;
    
    @Valid
    private List<RoutineRecordRequest> routineRecords;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReflectionRequest {
        private String content;
        private EmotionType emotion;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutineRecordRequest {
        private Long routineId;
        private PerformanceLevel performanceLevel;
    }
}