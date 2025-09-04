package com.groomthon.habiglow.domain.routine.entity;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.common.TargetType;
import com.groomthon.habiglow.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "routine_table", indexes = {
    @Index(name = "idx_routine_member", columnList = "member_id"),
    @Index(name = "idx_routine_category", columnList = "category")
})
public class RoutineEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Long routineId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
    
    @Embedded
    private RoutineDetails details;
    
    @Embedded
    private GrowthConfiguration growthConfiguration;
    
    /**
     * 루틴 정보 업데이트 (제목 제외)
     */
    public void updateRoutine(String description, RoutineCategory category, Boolean isGrowthMode,
                             TargetType targetType, Integer targetValue, Integer growthCycleDays, Integer targetIncrement) {
        // 기본 정보 업데이트
        this.details = this.details.updateDetails(description, category);
        
        // 성장 설정 업데이트
        if (Boolean.TRUE.equals(isGrowthMode)) {
            this.growthConfiguration = GrowthConfiguration.of(targetType, targetValue, growthCycleDays, targetIncrement);
        } else {
            this.growthConfiguration = GrowthConfiguration.disabled();
        }
    }
    
    /**
     * 성장 모드 활성화 여부 확인
     */
    public boolean isGrowthModeEnabled() {
        return growthConfiguration.isEnabled();
    }
    
    /**
     * 특정 카테고리에 속하는지 확인
     */
    public boolean belongsToCategory(RoutineCategory category) {
        return details.belongsToCategory(category);
    }
    
    /**
     * 루틴 소유자 확인
     */
    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }
    
    /**
     * 루틴 생성 팩토리 메서드
     */
    public static RoutineEntity createRoutine(MemberEntity member, String title, String description, 
                                            RoutineCategory category, Boolean isGrowthMode, 
                                            TargetType targetType, Integer targetValue, 
                                            Integer growthCycleDays, Integer targetIncrement) {
        RoutineDetails details = RoutineDetails.of(title, description, category);
        
        GrowthConfiguration growthConfiguration = Boolean.TRUE.equals(isGrowthMode) 
            ? GrowthConfiguration.of(targetType, targetValue, growthCycleDays, targetIncrement)
            : GrowthConfiguration.disabled();
            
        return RoutineEntity.builder()
                .member(member)
                .details(details)
                .growthConfiguration(growthConfiguration)
                .build();
    }

    public String getTitle() {
        return details.getTitle();
    }
    
    public String getDescription() {
        return details.getDescription();
    }
    
    public RoutineCategory getCategory() {
        return details.getCategory();
    }
    
    public Boolean getIsGrowthMode() {
        return growthConfiguration.getIsGrowthMode();
    }
    
    public TargetType getTargetType() {
        return growthConfiguration.getTargetType();
    }
    
    public Integer getTargetValue() {
        return growthConfiguration.getTargetValue();
    }
    
    public Integer getGrowthCycleDays() {
        return growthConfiguration.getGrowthCycleDays();
    }
    
    public Integer getTargetIncrement() {
        return growthConfiguration.getTargetIncrement();
    }
    
    public Integer getCurrentCycleDays() {
        return growthConfiguration.getCurrentCycleDays();
    }
    
    public GrowthConfiguration getGrowthConfiguration() {
        return growthConfiguration;
    }
    
    /**
     * GrowthConfiguration 업데이트 메서드
     */
    public void updateGrowthConfiguration(GrowthConfiguration newConfig) {
        this.growthConfiguration = newConfig;
    }
}