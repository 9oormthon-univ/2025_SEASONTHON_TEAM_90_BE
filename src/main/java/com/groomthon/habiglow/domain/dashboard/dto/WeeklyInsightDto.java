package com.groomthon.habiglow.domain.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(name = "WeeklyInsightDto", description = "주간 분석 결과 DTO")
public class WeeklyInsightDto {

    @Schema(description = "보고서 ID", example = "12")
    private Long id;

    @Schema(description = "사용자 ID", example = "1")
    private Long memberId;

    @Schema(description = "주 시작일(월요일 기준)", format = "date", example = "2025-08-25")
    private LocalDate weekStart;

    @Schema(description = "주 종료일(일요일 기준)", format = "date", example = "2025-08-31")
    private LocalDate weekEnd;

    @Schema(description = "입력 스냅샷(JSON 문자열이지만, 응답에선 객체로 출력)")
    @JsonRawValue
    private String inputSnapshotJson;

    @Schema(description = "AI 분석 결과(JSON 문자열이지만, 응답에선 객체로 출력)")
    @JsonRawValue
    private String insightJson;

    @Schema(description = "입력 스냅샷 해시(SHA-256)", example = "a2f9b3...c1")
    private String inputHash;

    @Schema(description = "사용 모델", example = "gpt-4o-mini")
    private String model;

    @Schema(description = "프롬프트 버전", example = "WKLY_V1")
    private String promptVersion;

    @Schema(description = "프롬프트 토큰 수", example = "842")
    private Integer promptTokens;

    @Schema(description = "완료 토큰 수", example = "128")
    private Integer completionTokens;

    @Schema(description = "생성 시각", example = "2025-08-31T18:16:19.573")
    private LocalDateTime createdAt;

    @Schema(description = "갱신 시각", example = "2025-08-31T18:16:19.573")
    private LocalDateTime updatedAt;

    public static WeeklyInsightDto from(WeeklyInsightEntity e) {
        return WeeklyInsightDto.builder()
                .id(e.getId())
                .memberId(e.getMemberId())
                .weekStart(e.getWeekStart())
                .weekEnd(e.getWeekEnd())
                .inputSnapshotJson(e.getInputSnapshotJson())
                .insightJson(e.getInsightJson())
                .inputHash(e.getInputHash())
                .model(e.getModel())
                .promptVersion(e.getPromptVersion())
                .promptTokens(e.getPromptTokens())
                .completionTokens(e.getCompletionTokens())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
