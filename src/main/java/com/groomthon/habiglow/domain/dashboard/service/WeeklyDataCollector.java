package com.groomthon.habiglow.domain.dashboard.service;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.util.WeeklyDummyDataGenerator;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 규칙
 * - 지난주(월~일): 실데이터가 있으면 실데이터, 없으면 더미
 * - 특정 주차: 지난주면 위 규칙 동일, 그 외는 실데이터만
 * - 이번주: 실데이터만 (더미 미적용)
 * - last-week-completed: 더미 프로필이면 true, 아니면 지난주 실데이터 존재 여부
 * - available-weeks: 실주차 + (더미이면) 지난주 더미 주차 합집합
 */
@Component
@RequiredArgsConstructor
public class WeeklyDataCollector {

    private final DailyReflectionRepository reflectionRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final WeeklyDummyDataGenerator dummyGenerator;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    private boolean isDummyProfile() {
        return activeProfiles != null && activeProfiles.contains("dummy-data");
    }

    private LocalDate thisMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate lastWeekMonday() {
        return thisMonday().minusWeeks(1);
    }

    /** 주차에 실데이터(회고/루틴)가 존재하는지 체크 */
    private boolean hasWeekData(Long memberId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        boolean hasReflections =
                reflectionRepository.existsByMember_IdAndReflectionDateBetween(memberId, weekStart, weekEnd);
        boolean hasRoutines =
                dailyRoutineRepository.existsByMember_IdAndPerformedDateBetween(memberId, weekStart, weekEnd);
        return hasReflections || hasRoutines;
    }


    /** 지난주 데이터 수집: 실우선, 없으면 더미 */
    public WeeklyAnalysisData collectLastWeekData(Long memberId) {
        LocalDate start = lastWeekMonday();
        if (isDummyProfile() && !hasWeekData(memberId, start)) {
            return dummyGenerator.generate(memberId, start);
        }
        return collectWeeklyData(memberId, start);
    }

    /** 특정 주차 데이터 수집: 지난주면 실우선/없으면 더미, 그 외는 실데이터만 */
    public WeeklyAnalysisData collectSpecificWeekData(Long memberId, LocalDate weekStart) {
        if (isDummyProfile() && weekStart.equals(lastWeekMonday()) && !hasWeekData(memberId, weekStart)) {
            return dummyGenerator.generate(memberId, weekStart);
        }
        return collectWeeklyData(memberId, weekStart);
    }

    /** 이번주 수집: 더미 미적용 */
    public WeeklyAnalysisData collectThisWeekData(Long memberId) {
        return collectWeeklyData(memberId, thisMonday());
    }

    /** 지난주 완료 여부 */
    public boolean isLastWeekCompleted(Long memberId) {
        if (isDummyProfile()) return true;
        return hasWeekData(memberId, lastWeekMonday());
    }

    /** 분석 가능한 주차: 실주차 + (더미일 때) 지난주 더미 주차 포함 */
    public List<String> getAvailableWeeks(Long memberId) {
        Set<String> weeks = reflectionRepository.findAllDatesByMemberId(memberId).stream()
                .map(d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                .map(monday -> DF.format(monday) + " ~ " + DF.format(monday.plusDays(6)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (isDummyProfile()) {
            LocalDate last = lastWeekMonday();
            weeks.add(DF.format(last) + " ~ " + DF.format(last.plusDays(6)));
        }
        return new ArrayList<>(weeks);
    }

    /** 실제 DB 기반 주간 데이터 수집(현재 스켈레톤; 빈 배열로 NPE 방지) */
    public WeeklyAnalysisData collectWeeklyData(Long memberId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        // 필요 시 실제 매핑 구현:
        // var reflections = reflectionRepository.findByMember_IdAndDateBetween(memberId, weekStart, weekEnd);
        // var routines    = dailyRoutineRepository.findByMember_IdAndPerformedDateBetween(memberId, weekStart, weekEnd);
        // TODO: reflections/routines → 날짜별 집계 → weekly.days 채우기

        return WeeklyAnalysisData.builder()
                .weekStart(DF.format(weekStart))
                .weekEnd(DF.format(weekEnd))
                .days(Collections.emptyList())
                .build();
    }
}
