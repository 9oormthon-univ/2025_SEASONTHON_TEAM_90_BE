package com.groomthon.habiglow.domain.routine.entity;

import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 루틴의 성장 모드 설정을 담당하는 Value Object
 * 성장 관련 로직과 검증을 캡슐화함
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrowthSettings {

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

    @Builder
    private GrowthSettings(Boolean isGrowthMode, TargetType targetType, Integer targetValue,
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

    /**
     * 성장 모드가 활성화되어 있는지 확인
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isGrowthMode);
    }

    /**
     * 목표치를 증가시킬 수 있는지 검증
     */
    public boolean canIncreaseTarget() {
        return isEnabled() && targetValue != null && targetIncrement != null && targetIncrement > 0;
    }

    /**
     * 목표치 증가 실행
     * @return 증가된 새로운 목표치
     */
    public Integer increaseTarget() {
        if (!canIncreaseTarget()) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_INCREASE_TARGET);
        }

        this.targetValue += targetIncrement;
        this.currentCycleDays = 0; // 성장 주기 리셋
        this.lastAdjustedDate = LocalDate.now(); // 조정 시점 기록
        return this.targetValue;
    }

    /**
     * 목표치 감소 실행 (서비스에서 계산된 값으로 설정)
     * @param newTargetValue 서비스에서 계산된 새로운 목표값
     */
    public void decreaseTarget(Integer newTargetValue) {
        if (newTargetValue == null || newTargetValue < (minimumTargetValue != null ? minimumTargetValue : 1)) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_DECREASE_TARGET);
        }

        this.targetValue = newTargetValue;
        this.currentCycleDays = 0; // 주기 리셋
        this.lastAdjustedDate = LocalDate.now(); // 조정 시점 기록
    }

    /**
     * 현재 주기 연속일 증가
     */
    public void incrementCurrentCycleDays() {
        this.currentCycleDays = (this.currentCycleDays != null ? this.currentCycleDays : 0) + 1;
    }

    /**
     * 현재 주기 연속일 리셋 (실패 시)
     */
    public void resetCurrentCycleDays() {
        this.currentCycleDays = 0;
    }

    /**
     * 성장 주기 완료 여부 확인
     */
    public boolean isGrowthCycleCompleted() {
        if (!isEnabled() || currentCycleDays == null || growthCycleDays == null) {
            return false;
        }
        return currentCycleDays >= growthCycleDays;
    }

    /**
     * 성장 설정 업데이트 (현재 주기일은 유지)
     */
    public GrowthSettings update(Boolean isGrowthMode, TargetType targetType, Integer targetValue,
        Integer growthCycleDays, Integer targetIncrement) {
        return GrowthSettings.builder()
            .isGrowthMode(isGrowthMode)
            .targetType(targetType)
            .targetValue(targetValue)
            .growthCycleDays(growthCycleDays)
            .targetIncrement(targetIncrement)
            .currentCycleDays(this.currentCycleDays) // 기존 주기일 유지
            .targetDecrement(this.targetDecrement) // 기존 감소값 유지
            .minimumTargetValue(this.minimumTargetValue) // 기존 최소값 유지
            .lastAdjustedDate(this.lastAdjustedDate) // 기존 조정 날짜 유지
            .build();
    }

    /**
     * 기본 성장 설정 (성장 모드 비활성화)
     */
    public static GrowthSettings disabled() {
        return GrowthSettings.builder()
            .isGrowthMode(false)
            .build();
    }

    /**
     * 성장 설정 생성
     */
    public static GrowthSettings of(TargetType targetType, Integer targetValue,
        Integer growthCycleDays, Integer targetIncrement) {
        return GrowthSettings.builder()
            .isGrowthMode(true)
            .targetType(targetType)
            .targetValue(targetValue)
            .growthCycleDays(growthCycleDays)
            .targetIncrement(targetIncrement)
            .targetDecrement(Math.max(1, targetIncrement / 2)) // 기본값: 증가량의 절반
            .minimumTargetValue(1) // 기본 최소값: 1
            .build();
    }

    /**
     * 완전한 성장 설정 생성 (감소 설정 포함)
     */
    public static GrowthSettings ofWithReduction(TargetType targetType, Integer targetValue,
        Integer growthCycleDays, Integer targetIncrement,
        Integer targetDecrement, Integer minimumTargetValue) {
        return GrowthSettings.builder()
            .isGrowthMode(true)
            .targetType(targetType)
            .targetValue(targetValue)
            .growthCycleDays(growthCycleDays)
            .targetIncrement(targetIncrement)
            .targetDecrement(targetDecrement)
            .minimumTargetValue(minimumTargetValue)
            .build();
    }

}