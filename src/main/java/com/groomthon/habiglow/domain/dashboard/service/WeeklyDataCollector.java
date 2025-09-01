package com.groomthon.habiglow.domain.dashboard.service;

import com.groomthon.habiglow.domain.dashboard.config.DashboardProperties;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WeeklyDataCollector {

    private final DailyReflectionRepository reflectionRepository;
    // 추후 실제 매핑 시 사용할 예정이라 주입만 유지
    private final DailyRoutineRepository dailyRoutineRepository;
    private final WeeklyDummyDataGenerator dummyGenerator;
    private final DashboardProperties dashboardProperties; // 추가된 의존성

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

    private String toRange(LocalDate monday) {
        return DF.format(monday) + " ~ " + DF.format(monday.plusDays(6));
    }

    /** 특정 날짜에 리플렉션 실데이터가 있는지(단건 조회로) */
    private boolean hasReflectionOn(Long memberId, LocalDate date) {
        return reflectionRepository.findByMemberIdAndReflectionDate(memberId, date).isPresent();
    }

    /** 외부에서 주차 실데이터 존재 여부가 필요할 때 사용 (월~일 7일 검사) */
    public boolean hasRealWeekData(Long memberId, LocalDate weekStart) {
        for (int i = 0; i < 7; i++) {
            if (hasReflectionOn(memberId, weekStart.plusDays(i))) return true;
        }
        return false;
    }

    /** 지난주: 실데이터 우선, 없으면 더미 */
    public WeeklyAnalysisData collectLastWeekData(Long memberId) {
        LocalDate start = lastWeekMonday();
        if (isDummyProfile() && !hasRealWeekData(memberId, start)) {
            return dummyGenerator.generate(memberId, start);
        }
        return collectWeeklyData(memberId, start);
    }

    /** 특정 주차: 지난주면 실우선/없으면 더미, 그 외 주차는 실데이터만 */
    public WeeklyAnalysisData collectSpecificWeekData(Long memberId, LocalDate weekStart) {
        if (isDummyProfile() && weekStart.equals(lastWeekMonday()) && !hasRealWeekData(memberId, weekStart)) {
            return dummyGenerator.generate(memberId, weekStart);
        }
        return collectWeeklyData(memberId, weekStart);
    }

    /** 이번주: 더미 미적용(실데이터만) */
    public WeeklyAnalysisData collectThisWeekData(Long memberId) {
        return collectWeeklyData(memberId, thisMonday());
    }

    /** 지난주 완료 여부 */
    public boolean isLastWeekCompleted(Long memberId) {
        if (isDummyProfile()) return true;
        return hasRealWeekData(memberId, lastWeekMonday());
    }

    /**
     * 분석 가능한 주차 목록 (하드코딩 제거)
     * - 더미 프로필이면: 설정된 더미 주차 수만큼 포함
     * - 설정된 기간만큼 뒤로 훑으면서 실데이터가 있는 주차만 포함
     */
    public List<String> getAvailableWeeks(Long memberId) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        LocalDate last = lastWeekMonday();

        // 더미 프로필인 경우 설정된 더미 주차 수만큼 포함
        if (isDummyProfile()) {
            int dummyWeeks = dashboardProperties.getWeek().getDummyWeeks();
            for (int i = 0; i < dummyWeeks; i++) {
                result.add(toRange(last.minusWeeks(i)));
            }
        }

        // 설정된 기간만큼 실데이터 주차 검색
        int lookbackWeeks = dashboardProperties.getWeek().getLookbackWeeks();
        for (int i = 0; i < lookbackWeeks; i++) {
            LocalDate monday = last.minusWeeks(i);
            if (hasRealWeekData(memberId, monday)) {
                result.add(toRange(monday));
            }
        }

        return new ArrayList<>(result);
    }

    /** 실제 DB 기반 주간 데이터 수집 (현재 스켈레톤; 빈 배열로 NPE 방지) */
    public WeeklyAnalysisData collectWeeklyData(Long memberId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        // 추후 실제 매핑 시:
        // var reflections = reflectionRepository.findByMemberIdAndDateBetween(memberId, weekStart, weekEnd);
        // var routines    = dailyRoutineRepository.findByMemberIdAndPerformedDateBetween(memberId, weekStart, weekEnd);
        // (리플렉션은 단건 메서드만 있으므로 날짜 루프 기반으로 집계하면 됩니다)

        return WeeklyAnalysisData.builder()
                .weekStart(DF.format(weekStart))
                .weekEnd(DF.format(weekEnd))
                .days(java.util.Collections.emptyList())
                .build();
    }
}