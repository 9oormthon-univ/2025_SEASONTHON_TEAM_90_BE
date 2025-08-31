package com.groomthon.habiglow.domain.dashboard.service;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyInsightService {

    private final WeeklyDataCollector dataCollector;
    private final OpenAiClient openAiClient; // 패키지 경로는 프로젝트에 맞게

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

    public List<String> getAvailableWeeks(Long memberId) {
        return dataCollector.getAvailableWeeks(memberId);
    }

    /** 컨트롤러에서 주차 실데이터 존재 여부 체크용 */
    public boolean hasRealWeekData(Long memberId, LocalDate weekStart) {
        return dataCollector.hasRealWeekData(memberId, weekStart);
    }

    private void validateSkeleton(WeeklyAnalysisData weekly) {
        if (weekly == null || weekly.getWeekStart() == null || weekly.getWeekEnd() == null) {
            throw new IllegalStateException("주간 데이터 스켈레톤이 유효하지 않습니다.");
        }
    }
}
