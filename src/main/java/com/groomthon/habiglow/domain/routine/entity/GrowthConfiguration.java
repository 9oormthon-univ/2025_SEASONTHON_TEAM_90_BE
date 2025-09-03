package com.groomthon.habiglow.domain.routine.entity;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.routine.common.TargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 루틴의 성장 모드 설정을 담당하는 단순화된 Value Object
 * 데이터 중심으로 구성되며, 복잡한 비즈니스 로직은 서비스 레이어에서 처리
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrowthConfiguration {

    @Column(name = "is_growth_mode", nullable = false)
    private Boolean isGrowthMode = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;

    @Column(name = "target_value")
    private Integer targetValue;

    @Column(name = "growth_cycle_days")
    private Integer growthCycleDays;

    @Column(name = "target_increment")
    private Integer targetIncrement;

    @Column(name = "current_cycle_days")
    private Integer currentCycleDays = 0;

    @Column(name = "target_decrement")
    private Integer targetDecrement;

    @Column(name = "minimum_target_value")
    private Integer minimumTargetValue;

    @Column(name = "last_adjusted_date")
    private LocalDate lastAdjustedDate;

    @Builder(toBuilder = true)
    private GrowthConfiguration(Boolean isGrowthMode, TargetType targetType, Integer targetValue,
        Integer growthCycleDays, Integer targetIncrement, Integer currentCycleDays,
        Integer targetDecrement, Integer minimumTargetValue, LocalDate lastAdjustedDate) {
        this.isGrowthMode = isGrowthMode != null ? isGrowthMode : false;
        this.targetType = targetType;
        this.targetValue = targetValue;
        this.growthCycleDays = growthCycleDays;
        this.targetIncrement = targetIncrement;
        this.currentCycleDays = currentCycleDays != null ? currentCycleDays : 0;
        this.targetDecrement = targetDecrement;
        this.minimumTargetValue = minimumTargetValue != null ? minimumTargetValue : 1;
        this.lastAdjustedDate = lastAdjustedDate;
    }

    // 단순한 상태 확인 메서드만 유지
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isGrowthMode);
    }

    public boolean isCycleCompleted() {
        if (!isEnabled() || currentCycleDays == null || growthCycleDays == null) {
            return false;
        }
        return currentCycleDays >= growthCycleDays;
    }

    // 데이터 업데이트 메서드 (비즈니스 로직 없는 단순 setter)
    public GrowthConfiguration withUpdatedTarget(Integer newTargetValue) {
        return this.toBuilder()
            .targetValue(newTargetValue)
            .currentCycleDays(0) // 목표 변경 시 주기 리셋
            .lastAdjustedDate(LocalDate.now())
            .build();
    }

    public GrowthConfiguration withResetCycle() {
        return this.toBuilder()
            .currentCycleDays(0)
            .build();
    }

    public GrowthConfiguration withIncrementedCycle() {
        return this.toBuilder()
            .currentCycleDays((this.currentCycleDays != null ? this.currentCycleDays : 0) + 1)
            .build();
    }

    // 팩토리 메서드
    public static GrowthConfiguration disabled() {
        return GrowthConfiguration.builder()
            .isGrowthMode(false)
            .build();
    }

    public static GrowthConfiguration of(TargetType targetType, Integer targetValue,
        Integer growthCycleDays, Integer targetIncrement) {
        return GrowthConfiguration.builder()
            .isGrowthMode(true)
            .targetType(targetType)
            .targetValue(targetValue)
            .growthCycleDays(growthCycleDays)
            .targetIncrement(targetIncrement)
            .targetDecrement(Math.max(1, targetIncrement / 2))
            .minimumTargetValue(1)
            .build();
    }
}