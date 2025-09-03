package com.groomthon.habiglow.domain.routine.dto.request;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.common.TargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "루틴 생성 요청")
public class CreateRoutineRequest {
    
    @NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "루틴 카테고리", example = "HEALTH")
    private RoutineCategory category;
    
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다")
    @Schema(description = "루틴 제목", example = "매일 운동하기")
    private String title;
    
    @Size(max = 500, message = "설명은 500자 이하여야 합니다")
    @Schema(description = "루틴 설명", example = "건강한 생활을 위해 매일 30분씩 운동하기")
    private String description;
    
    @Schema(description = "성장 모드 활성화 여부", example = "true")
    private Boolean isGrowthMode = false;
    
    @Schema(description = "목표 타입 (성장 모드일 때 필수)", example = "NUMBER")
    private TargetType targetType;
    
    @Schema(description = "목표 수치 (성장 모드일 때 필수)", example = "500")
    private Integer targetValue;
    
    @Schema(description = "성장 주기(일) (성장 모드일 때 필수)", example = "7")
    private Integer growthCycleDays;
    
    @Schema(description = "목표 증가 수치 (성장 모드일 때 필수)", example = "50")
    private Integer targetIncrement;
}