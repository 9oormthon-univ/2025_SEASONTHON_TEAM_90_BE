package com.groomthon.habiglow.domain.daily.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmotionType {
    
    HAPPY("행복"),
    SOSO("그저그래"),
    SAD("슬픔"),
    MAD("화남");
    
    private final String description;
}