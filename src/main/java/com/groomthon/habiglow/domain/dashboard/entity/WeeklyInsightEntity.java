package com.groomthon.habiglow.domain.dashboard.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_insights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "week_start"}))
@Getter
@Setter
@Schema(name = "WeeklyInsightEntity", hidden = true) // 엔티티는 문서에서 숨김(필요시 해제)
public class WeeklyInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="member_id", nullable=false)
    private Long memberId;

    @Column(name="week_start", nullable=false)
    private LocalDate weekStart;

    @Column(name="week_end", nullable=false)
    private LocalDate weekEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="input_snapshot_json", columnDefinition="jsonb")
    private String inputSnapshotJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="insight_json", columnDefinition="jsonb")
    private String insightJson;

    @Column(name="input_hash", length=64, nullable=false)
    private String inputHash;

    private String model;
    private String promptVersion;
    private Integer promptTokens;
    private Integer completionTokens;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
