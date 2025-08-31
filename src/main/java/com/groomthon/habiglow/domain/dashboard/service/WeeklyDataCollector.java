package com.groomthon.habiglow.domain.dashboard.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.groomthon.habiglow.domain.dashboard.util.WeeklyDummyDataGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.EmotionType;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData.DayData;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData.RoutineResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주간 AI 분석을 위한 데이터 수집 서비스
 * 월~일 7일 주기로 회고 및 루틴 데이터를 수집하여 AI 분석용 JSON 포맷으로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyDataCollector {

    private final DailyReflectionRepository reflectionRepository;
    private final DailyRoutineRepository routineRepository;

    /**
     * 특정 주의 데이터를 수집 (월-일)
     */
    public WeeklyAnalysisData collectWeeklyData(Long memberId, LocalDate weekStart) {
        validateWeekStart(weekStart);

        LocalDate weekEnd = weekStart.plusDays(6);
        List<DayData> days = new ArrayList<>();

        log.info("주간 데이터 수집 시작: {} ~ {} (회원ID: {})", weekStart, weekEnd, memberId);

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate currentDate = weekStart.plusDays(dayOffset);
            DayData dayData = collectDayData(memberId, currentDate);
            days.add(dayData);
        }

        log.info("주간 데이터 수집 완료: 총 {}일 데이터", days.size());

        return WeeklyAnalysisData.builder()
                .weekStart(weekStart.toString())
                .weekEnd(weekEnd.toString())
                .days(days)
                .build();
    }

    /**
     * 지난주 데이터를 수집 (이번주 월요일 기준 지난주 월~일)
     */
    /**
     * 지난주 데이터를 수집 (이번주 월요일 기준 지난주 월~일)
     */
    public WeeklyAnalysisData collectLastWeekData(Long memberId) {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekMonday = thisMonday.minusWeeks(1);

        // ✅ 실제 DB 대신 더미 데이터 반환 (Swagger 테스트용)
        return WeeklyDummyDataGenerator.generate(memberId, lastWeekMonday);

        // 🔽 실제 DB 쓰려면 기존 코드 사용
        // return collectWeeklyData(memberId, lastWeekMonday);
    }


    /**
     * 이번주 데이터를 수집 (월~현재까지)
     */
    public WeeklyAnalysisData collectThisWeekData(Long memberId) {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return collectWeeklyData(memberId, thisMonday);
    }

    private DayData collectDayData(Long memberId, LocalDate date) {
        // 하루 회고 데이터
        DailyReflectionEntity reflection = reflectionRepository
                .findByMemberIdAndReflectionDate(memberId, date)
                .orElse(null);

        // 하루 루틴 기록들
        List<DailyRoutineEntity> routineRecords = routineRepository
                .findByMemberIdAndPerformedDateWithRoutine(memberId, date);

        String emotion = mapEmotionToEmoji(reflection != null ? reflection.getEmotion() : EmotionType.SOSO);
        String note = reflection != null ? reflection.getReflectionContent() : "기록 없음";

        List<RoutineResult> routines = routineRecords.stream()
                .map(this::mapRoutineToResult)
                .collect(Collectors.toList());

        return DayData.builder()
                .date(date.toString())
                .emotion(emotion)
                .routines(routines)
                .note(note)
                .build();
    }

    private RoutineResult mapRoutineToResult(DailyRoutineEntity routineRecord) {
        String result = mapPerformanceToResult(routineRecord.getPerformanceLevel());
        String routineName = routineRecord.getRoutineTitle();

        return RoutineResult.builder()
                .name(routineName)
                .result(result)
                .build();
    }

    /**
     * 감정 타입을 이모지로 변환
     */
    private String mapEmotionToEmoji(EmotionType emotionType) {
        return switch (emotionType) {
            case HAPPY -> "😀";
            case SOSO -> "🙂";
            case SAD -> "😐";
            case MAD -> "☁️";
        };
    }

    /**
     * 성과 레벨을 결과로 변환
     */
    private String mapPerformanceToResult(PerformanceLevel performanceLevel) {
        return switch (performanceLevel) {
            case FULL_SUCCESS -> "SUCCESS";
            case PARTIAL_SUCCESS -> "PARTIAL";
            case NOT_PERFORMED -> "FAIL";
        };
    }

    private void validateWeekStart(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("주간 데이터는 월요일부터 시작해야 합니다. 입력된 날짜: " + weekStart);
        }
    }

    /**
     * 회원의 분석 가능한 주차 목록 조회 (최근 4주)
     */
    public List<LocalDate> getAvailableWeeks(Long memberId) {
        LocalDate currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDate> weeks = new ArrayList<>();

        // 최근 4주치 월요일 날짜 생성
        for (int i = 0; i < 4; i++) {
            LocalDate weekStart = currentMonday.minusWeeks(i);

            // 해당 주에 데이터가 있는지 간단 체크
            boolean hasData = hasWeeklyData(memberId, weekStart);
            if (hasData) {
                weeks.add(weekStart);
            }
        }

        return weeks;
    }

    private boolean hasWeeklyData(Long memberId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        // 해당 주차에 회고나 루틴 기록이 하나라도 있는지 확인
        long reflectionCount = reflectionRepository.findAll().stream()
                .filter(r -> r.getMember().getId().equals(memberId))
                .filter(r -> !r.getReflectionDate().isBefore(weekStart))
                .filter(r -> !r.getReflectionDate().isAfter(weekEnd))
                .count();

        long routineCount = routineRepository.findAll().stream()
                .filter(r -> r.getMember().getId().equals(memberId))
                .filter(r -> !r.getPerformedDate().isBefore(weekStart))
                .filter(r -> !r.getPerformedDate().isAfter(weekEnd))
                .count();

        return reflectionCount > 0 || routineCount > 0;
    }
}