package com.groomthon.habiglow.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "일별 완성률/감정")
public class DailyCompletionInfo {
    private LocalDate date;
    private int done;
    private int total;
    private double rate;
    private String mood;     // "HAPPY"/"SOSO"/"SAD"/"MAD"
    private boolean isFuture; // 이번 주에서 아직 오지 않은 날
}