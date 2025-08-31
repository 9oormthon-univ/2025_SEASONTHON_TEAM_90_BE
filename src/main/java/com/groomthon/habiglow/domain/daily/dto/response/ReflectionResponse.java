package com.groomthon.habiglow.domain.daily.dto.response;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.EmotionType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReflectionResponse {
    
    private String content;
    private EmotionType emotion;
    private LocalDate reflectionDate;
    
    public static ReflectionResponse from(DailyReflectionEntity entity) {
        return new ReflectionResponse(
            entity.getReflectionContent(),
            entity.getEmotion(),
            entity.getReflectionDate()
        );
    }
}