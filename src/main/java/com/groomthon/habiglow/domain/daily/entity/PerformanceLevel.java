package com.groomthon.habiglow.domain.daily.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceLevel {
    
    FULL_SUCCESS("완전성공"),
    PARTIAL_SUCCESS("부분성공"),
    NOT_PERFORMED("미수행");
    
    private final String description;
}