package com.groomthon.habiglow.domain.dashboard.service;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.domain.dashboard.service.WeeklyDataCollector;
import com.groomthon.habiglow.domain.dashboard.service.OpenAiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyInsightService {

    private final WeeklyDataCollector dataCollector;
    private final OpenAiClient openAiClient;

    public WeeklyInsightResponse generateLastWeekInsight(Long memberId) {
        WeeklyAnalysisData data = dataCollector.collectLastWeekData(memberId);
        validateSkeleton(data);
        return openAiClient.generateWeeklyInsight(data);
    }

    public WeeklyInsightResponse generateSpecificWeekInsight(Long memberId, LocalDate weekStart) {
        WeeklyAnalysisData data = dataCollector.collectSpecificWeekData(memberId, weekStart);
        validateSkeleton(data);
        return openAiClient.generateWeeklyInsight(data);
    }

    public WeeklyInsightResponse generateThisWeekInsight(Long memberId) {
        WeeklyAnalysisData data = dataCollector.collectThisWeekData(memberId);
        validateSkeleton(data);
        return openAiClient.generateWeeklyInsight(data);
    }

    public boolean isLastWeekCompleted(Long memberId) {
        return dataCollector.isLastWeekCompleted(memberId);
    }

    public java.util.List<String> getAvailableWeeks(Long memberId) {
        return dataCollector.getAvailableWeeks(memberId);
    }

    private void validateSkeleton(WeeklyAnalysisData weekly) {
        if (weekly == null || weekly.getWeekStart() == null || weekly.getWeekEnd() == null) {
            throw new IllegalStateException("주간 데이터 스켈레톤이 유효하지 않습니다.");
        }
    }
}
