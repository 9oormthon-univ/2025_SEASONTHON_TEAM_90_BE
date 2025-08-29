package com.groomthon.habiglow.domain.routine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 루틴의 기본 정보를 담당하는 Value Object
 * 제목, 설명, 카테고리 등 기본 속성을 캡슐화함
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineDetails {
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutineCategory category;
    
    @Builder
    private RoutineDetails(String title, String description, RoutineCategory category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }
    
    /**
     * 루틴 상세 정보 업데이트 (제목 제외)
     * 제목은 루틴 생성 후 변경할 수 없음
     */
    public RoutineDetails updateDetails(String description, RoutineCategory category) {
        return RoutineDetails.builder()
                .title(this.title) // 기존 제목 유지
                .description(description)
                .category(category)
                .build();
    }
    
    /**
     * 카테고리 변경
     */
    public RoutineDetails changeCategory(RoutineCategory newCategory) {
        return RoutineDetails.builder()
                .title(this.title)
                .description(this.description)
                .category(newCategory)
                .build();
    }
    
    /**
     * 설명 변경
     */
    public RoutineDetails changeDescription(String newDescription) {
        return RoutineDetails.builder()
                .title(this.title)
                .description(newDescription)
                .category(this.category)
                .build();
    }
    
    /**
     * 루틴이 특정 카테고리에 속하는지 확인
     */
    public boolean belongsToCategory(RoutineCategory category) {
        return this.category == category;
    }
    
    /**
     * 루틴 세부사항 생성
     */
    public static RoutineDetails of(String title, String description, RoutineCategory category) {
        return RoutineDetails.builder()
                .title(title)
                .description(description)
                .category(category)
                .build();
    }
    
}