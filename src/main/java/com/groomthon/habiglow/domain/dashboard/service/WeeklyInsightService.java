package com.groomthon.habiglow.domain.dashboard.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HabiGlow AI 주간 분석 서비스
 *
 * 월~일 7일 주기로 사용자의 습관 데이터를 분석하여
 * 감정 변화 패턴과 루틴 성공/실패에 대한 공감과 응원 메시지 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyInsightService {

    private final WeeklyDataCollector dataCollector;
    private final OpenAiClient openAiClient;

    /**
     * 지난주 데이터 기반 AI 분석 생성
     * (이번주 월요일 기준 지난주 월~일)
     */
    public WeeklyInsightResponse generateLastWeekInsight(Long memberId) {
        log.info("지난주 AI 분석 요청 - 회원ID: {}", memberId);

        WeeklyAnalysisData weeklyData = dataCollector.collectLastWeekData(memberId);

        validateWeeklyData(weeklyData);

        WeeklyInsightResponse insight = openAiClient.generateWeeklyInsight(weeklyData);

        log.info("지난주 AI 분석 완료 - 회원ID: {}, 분석 기간: {}",
                memberId, insight.getWeekRange());

        return insight;
    }

    /**
     * 특정 주차 데이터 기반 AI 분석 생성
     */
    public WeeklyInsightResponse generateWeekInsight(Long memberId, LocalDate weekStart) {
        log.info("특정 주차 AI 분석 요청 - 회원ID: {}, 주차: {}", memberId, weekStart);

        validateWeekStart(weekStart);

        WeeklyAnalysisData weeklyData = dataCollector.collectWeeklyData(memberId, weekStart);

        validateWeeklyData(weeklyData);

        WeeklyInsightResponse insight = openAiClient.generateWeeklyInsight(weeklyData);

        log.info("특정 주차 AI 분석 완료 - 회원ID: {}, 분석 기간: {}",
                memberId, insight.getWeekRange());

        return insight;
    }

    /**
     * 이번주 진행중 데이터 기반 AI 분석 생성 (참고용)
     */
    public WeeklyInsightResponse generateThisWeekInsight(Long memberId) {
        log.info("이번주 진행상황 AI 분석 요청 - 회원ID: {}", memberId);

        WeeklyAnalysisData weeklyData = dataCollector.collectThisWeekData(memberId);

        validateWeeklyData(weeklyData);

        WeeklyInsightResponse insight = openAiClient.generateWeeklyInsight(weeklyData);

        log.info("이번주 진행상황 AI 분석 완료 - 회원ID: {}, 분석 기간: {}",
                memberId, insight.getWeekRange());

        return insight;
    }

    /**
     * 분석 가능한 주차 목록 조회
     */
    public List<LocalDate> getAvailableWeeks(Long memberId) {
        log.debug("분석 가능한 주차 목록 조회 - 회원ID: {}", memberId);

        return dataCollector.getAvailableWeeks(memberId);
    }

    /**
     * 현재 시점에서 지난주가 완료되었는지 확인
     */
    public boolean isLastWeekCompleted() {
        LocalDate today = LocalDate.now();
        DayOfWeek currentDayOfWeek = today.getDayOfWeek();

        // 월요일 이후면 지난주가 완료되었다고 판단
        return currentDayOfWeek != DayOfWeek.SUNDAY;
    }

    private void validateWeekStart(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BaseException(ErrorCode.INVALID_WEEK_START);
        }

        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        if (weekStart.isAfter(thisMonday)) {
            throw new BaseException(ErrorCode.FUTURE_WEEK_NOT_ALLOWED);
        }
    }

    private void validateWeeklyData(WeeklyAnalysisData weeklyData) {
        if (weeklyData.getDays() == null || weeklyData.getDays().isEmpty()) {
            throw new BaseException(ErrorCode.NO_WEEKLY_DATA_FOUND);
        }

        // 최소한의 의미있는 데이터가 있는지 확인
        boolean hasAnyData = weeklyData.getDays().stream()
                .anyMatch(day ->
                        (day.getRoutines() != null && !day.getRoutines().isEmpty()) ||
                                (day.getNote() != null && !day.getNote().equals("기록 없음"))
                );

        if (!hasAnyData) {
            throw new BaseException(ErrorCode.INSUFFICIENT_DATA_FOR_ANALYSIS);
        }

        log.debug("주간 데이터 검증 완료 - 일수: {}, 분석 기간: {} ~ {}",
                weeklyData.getDays().size(), weeklyData.getWeekStart(), weeklyData.getWeekEnd());
    }
}