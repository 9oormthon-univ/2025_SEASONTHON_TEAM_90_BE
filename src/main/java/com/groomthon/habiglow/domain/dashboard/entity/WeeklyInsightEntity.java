package com.groomthon.habiglow.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주간 AI 분석 결과 저장 엔티티
 * 주차별로 1회 생성되어 영구 저장
 */
@Entity
@Table(
        name = "weekly_insights",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"member_id", "week_start_date"},
                name = "uk_weekly_insights_member_week"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WeeklyInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    // AI 분석 결과 필드들
    @Column(name = "week_range", nullable = false, length = 50)
    private String weekRange;

    @Column(name = "mood_daily", nullable = false, columnDefinition = "JSON")
    private String moodDaily; // JSON 배열로 저장

    @Column(name = "mood_trend", nullable = false, length = 10)
    private String moodTrend;

    @Column(name = "weekly_summary", nullable = false, length = 100)
    private String weeklySummary;

    @Column(name = "good_points", nullable = false, columnDefinition = "JSON")
    private String goodPoints; // JSON 배열로 저장

    @Column(name = "failure_patterns", nullable = false, columnDefinition = "JSON")
    private String failurePatterns; // JSON 배열로 저장

    @Column(name = "empathy", nullable = false, length = 100)
    private String empathy;

    @Column(name = "encouragement", nullable = false, length = 100)
    private String encouragement;

    @Column(name = "analysis_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AnalysisType analysisType;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public WeeklyInsightEntity(Long memberId, LocalDate weekStartDate, LocalDate weekEndDate,
                               String weekRange, String moodDaily, String moodTrend,
                               String weeklySummary, String goodPoints, String failurePatterns,
                               String empathy, String encouragement, AnalysisType analysisType) {
        this.memberId = memberId;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.weekRange = weekRange;
        this.moodDaily = moodDaily;
        this.moodTrend = moodTrend;
        this.weeklySummary = weeklySummary;
        this.goodPoints = goodPoints;
        this.failurePatterns = failurePatterns;
        this.empathy = empathy;
        this.encouragement = encouragement;
        this.analysisType = analysisType;
    }

    public enum AnalysisType {
        LAST_WEEK,      // 지난주 분석
        SPECIFIC_WEEK,  // 특정 주차 분석
        CURRENT_WEEK    // 이번주 진행상황
    }
}