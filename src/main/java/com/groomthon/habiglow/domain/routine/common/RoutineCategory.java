package com.groomthon.habiglow.domain.routine.common;

import lombok.Getter;

@Getter
public enum RoutineCategory {
    HABIT_IMPROVEMENT("습관 개선"),
    HEALTH("건강"), 
    LEARNING("학습"),
    MINDFULNESS("마음 챙김"),
    EXPENSE_MANAGEMENT("소비 관리"),
    HOBBY("취미"),
    DIET("식습관"),
    SLEEP("수면"),
    SELF_CARE("자기관리");

    private final String description;

    RoutineCategory(String description) {
        this.description = description;
    }

}