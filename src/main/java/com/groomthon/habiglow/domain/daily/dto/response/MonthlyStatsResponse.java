package com.groomthon.habiglow.domain.daily.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyStatsResponse {
    
    private int year;
    private int month;
    private List<DailyStat> dailyStats;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyStat {
        private int day;
        private int successfulRoutines;
        private int totalRoutines;
        private double successRate;
    }
}