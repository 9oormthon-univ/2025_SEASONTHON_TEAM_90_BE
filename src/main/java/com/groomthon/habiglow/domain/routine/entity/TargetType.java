package com.groomthon.habiglow.domain.routine.entity;

public enum TargetType {
    DATE("날짜"),
    NUMBER("숫자");

    private final String description;

    TargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}