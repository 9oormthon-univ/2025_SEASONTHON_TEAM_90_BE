package com.groomthon.habiglow.domain.daily.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmotionType {
    
    LOW("낮음"),
    NORMAL("보통"),
    GOOD("좋음"),
    VERY_GOOD("매우 좋음");
    
    private final String description;
    
    /**
     * 감정을 퍼센테이지로 변환 (UI 표시용)
     */
    public int getPercentage() {
        return switch (this) {
            case LOW -> 25;         // 맨 아래 줄
            case NORMAL -> 50;      // 2번째 줄  
            case GOOD -> 75;        // 3번째 줄
            case VERY_GOOD -> 100;  // 맨 위 줄
        };
    }
}