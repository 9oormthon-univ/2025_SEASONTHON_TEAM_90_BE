package com.groomthon.habiglow.domain.routine.common;

public enum TargetType {
    DATE("날짜", "일"),
    NUMBER("숫자", "개");

    private final String description;
    private final String unit;

    TargetType(String description, String unit) {
        this.description = description;
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }
    
    public String getUnit() {
        return unit;
    }
}