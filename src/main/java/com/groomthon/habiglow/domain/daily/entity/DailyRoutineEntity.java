package com.groomthon.habiglow.domain.daily.entity;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.entity.TargetType;
import com.groomthon.habiglow.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_routine_table", 
       indexes = {
           @Index(name = "idx_daily_routine_member_date", columnList = "member_id, performed_date"),
           @Index(name = "idx_daily_routine_routine_date", columnList = "routine_id, performed_date")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_member_routine_date", columnNames = {"member_id", "routine_id", "performed_date"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DailyRoutineEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_routine_id")
    private Long dailyRoutineId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = true)
    private RoutineEntity routine;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "performance_level", nullable = false)
    private PerformanceLevel performanceLevel;
    
    @Column(name = "consecutive_days", nullable = false)
    @Builder.Default
    private Integer consecutiveDays = 0;
    
    @Column(name = "performed_date", nullable = false)
    private LocalDate performedDate;
    
    @Column(name = "routine_title", length = 100)
    private String routineTitle;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "routine_category")
    private RoutineCategory routineCategory;
    
    @Column(name = "is_growth_mode")
    private Boolean isGrowthMode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;
    
    @Column(name = "target_value")
    private Integer targetValue;
    
    @Column(name = "growth_cycle_days")
    private Integer growthCycleDays;
    
    @Column(name = "target_increment")
    private Integer targetIncrement;
    
    public void updatePerformance(PerformanceLevel performanceLevel, Integer consecutiveDays) {
        this.performanceLevel = performanceLevel;
        this.consecutiveDays = consecutiveDays;
    }
    
    public boolean isFullSuccess() {
        return this.performanceLevel == PerformanceLevel.FULL_SUCCESS;
    }
    
    public static DailyRoutineEntity create(RoutineEntity routine, MemberEntity member, 
                                           PerformanceLevel performance, LocalDate date, Integer consecutiveDays) {
        return DailyRoutineEntity.builder()
                .routine(routine)
                .member(member)
                .performanceLevel(performance)
                .performedDate(date)
                .consecutiveDays(consecutiveDays)
                .routineTitle(routine.getTitle())
                .routineCategory(routine.getCategory())
                .isGrowthMode(routine.getIsGrowthMode())
                .targetType(routine.getTargetType())
                .targetValue(routine.getTargetValue())
                .growthCycleDays(routine.getGrowthCycleDays())
                .targetIncrement(routine.getTargetIncrement())
                .build();
    }
}