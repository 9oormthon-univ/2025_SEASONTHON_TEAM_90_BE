package com.groomthon.habiglow.domain.daily.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.routine.dto.response.RoutineResponse;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DailyRecordResponse {
    
    private ReflectionResponse reflection;
    private List<RoutineRecordResponse> routineRecords;
    private List<RoutineResponse> allRoutines;
    
    public static DailyRecordResponse of(DailyReflectionEntity reflection, 
                                        List<DailyRoutineEntity> records,
                                        List<RoutineEntity> allRoutines) {
        
        ReflectionResponse reflectionDto = reflection != null ? 
            ReflectionResponse.from(reflection) : null;
        
        List<RoutineRecordResponse> recordDtos = records.stream()
            .map(RoutineRecordResponse::from)
            .collect(Collectors.toList());
        
        List<RoutineResponse> routineDtos = allRoutines.stream()
            .map(RoutineResponse::from)
            .collect(Collectors.toList());
        
        return new DailyRecordResponse(reflectionDto, recordDtos, routineDtos);
    }
    
    public static DailyRecordResponse of(DailyReflectionEntity reflection, 
                                        List<DailyRoutineEntity> records) {
        
        ReflectionResponse reflectionDto = reflection != null ? 
            ReflectionResponse.from(reflection) : null;
        
        List<RoutineRecordResponse> recordDtos = records.stream()
            .map(RoutineRecordResponse::from)
            .collect(Collectors.toList());
        
        return new DailyRecordResponse(reflectionDto, recordDtos, List.of());
    }
}