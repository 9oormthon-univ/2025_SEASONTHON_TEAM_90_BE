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
}