package com.groomthon.habiglow.domain.dashboard.service;

import com.groomthon.habiglow.domain.dashboard.dto.*;
import com.groomthon.habiglow.domain.dashboard.util.WeeklyDummyDataGenerator;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.common.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel; // 엔티티 패키지와 일치!
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyDashboardService {

    private final RoutineRepository routineRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final DailyReflectionRepository dailyReflectionRepository;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DUMMY_MIN_TOTAL_ROUTINES = 6; // 루틴 0개여도 더미 주차를 빈 값으로 만들지 않기 위한 최소 표시 개수

    public WeeklyDashboardDto getView(Long memberId, LocalDate weekStart) {
        LocalDate thisMon = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekMon = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekSun = weekMon.plusDays(6);

        boolean isCurrentWeek = weekMon.equals(thisMon);
        boolean isComplete = weekMon.isBefore(thisMon);
        boolean isFutureWeek = weekMon.isAfter(thisMon);
        boolean dummyOn = activeProfiles != null && activeProfiles.contains("dummy-data");
        boolean isLastWeek = weekMon.equals(thisMon.minusWeeks(1));

        NavInfo nav = NavInfo.builder()
                .hasPrev(true)
                .hasNext(!isCurrentWeek && !isFutureWeek)
                .prevWeekStart(weekMon.minusWeeks(1))
                .nextWeekStart(weekMon.plusWeeks(1))
                .build();

        String label = weekMon.getMonthValue() + "월 " + ordinalOfWeekInMonth(weekMon) + "째 주";

        // 사용자 설정 루틴
        List<RoutineEntity> myRoutines = routineRepository.findByMember_Id(memberId);
        int realTotalRoutines = myRoutines.size();

        // 지난 주 + dummy ON → 더미, 그 외 → 실데이터
        List<DailyCompletionInfo> dailyCompletion;
        Map<String, Integer> emotionDist;
        MetricsInfo metrics;

        if (!(dummyOn && isLastWeek)) {
            // ===== 실데이터 경로 =====
            List<DailyRoutineEntity> routineRecords =
                    dailyRoutineRepository.findByMemberIdAndPerformedDateBetween(memberId, weekMon, weekSun);

            List<DailyReflectionEntity> reflections =
                    dailyReflectionRepository.findByMemberIdAndReflectionDateBetweenOrderByReflectionDateAsc(
                            memberId, weekMon, weekSun);

            var byDate = routineRecords.stream()
                    .collect(Collectors.groupingBy(DailyRoutineEntity::getPerformedDate));

            var reflectionsByDate = reflections.stream()
                    .collect(Collectors.toMap(DailyReflectionEntity::getReflectionDate,
                            Function.identity(), (a, b) -> a, TreeMap::new));

            dailyCompletion = new ArrayList<>();
            int overallDone = 0, overallTotal = 0;
            Map<RoutineCategory, int[]> catAgg = new EnumMap<>(RoutineCategory.class);

            for (int i = 0; i < 7; i++) {
                LocalDate d = weekMon.plusDays(i);
                List<DailyRoutineEntity> dayRecords = byDate.getOrDefault(d, List.of());

                int done = (int) dayRecords.stream()
                        .filter(r -> r.getPerformanceLevel() != PerformanceLevel.NOT_PERFORMED)
                        .count();
                int total = dayRecords.size();
                double rate = total == 0 ? 0.0 : round1(100.0 * done / total);
                boolean isFuture = isCurrentWeek && d.isAfter(LocalDate.now(KST));

                String moodName = Optional.ofNullable(reflectionsByDate.get(d))
                        .map(r -> r.getEmotion() == null ? null : r.getEmotion().name())
                        .orElse(null);

                dailyCompletion.add(DailyCompletionInfo.builder()
                        .date(d)
                        .done(done)
                        .total(total)
                        .rate(rate)
                        .mood(moodName)
                        .isFuture(isFuture)
                        .build());

                overallDone += done;
                overallTotal += total;

                for (DailyRoutineEntity rec : dayRecords) {
                    RoutineCategory cat = rec.getRoutineCategory();
                    catAgg.computeIfAbsent(cat, k -> new int[2]);
                    if (rec.getPerformanceLevel() != PerformanceLevel.NOT_PERFORMED) {
                        catAgg.get(cat)[0] += 1; // done
                    }
                    catAgg.get(cat)[1] += 1;     // total
                }
            }

            emotionDist = buildEmotionDistribution(dailyCompletion);

            Rate overall = Rate.builder()
                    .done(overallDone)
                    .total(overallTotal)
                    .rate(overallTotal == 0 ? 0.0 : round1(100.0 * overallDone / overallTotal))
                    .build();

            List<CategoryRate> categories = catAgg.entrySet().stream()
                    .map(e -> CategoryRate.builder()
                            .code(e.getKey().name())
                            .label(categoryLabel(e.getKey()))
                            .done(e.getValue()[0])
                            .total(e.getValue()[1])
                            .rate(e.getValue()[1] == 0 ? 0.0 : round1(100.0 * e.getValue()[0] / e.getValue()[1]))
                            .build())
                    .sorted(Comparator.comparing(CategoryRate::getCode))
                    .toList();

            metrics = MetricsInfo.builder()
                    .totalRoutines(realTotalRoutines)
                    .overall(overall)
                    .categories(categories)
                    .build();

        } else {
            // ===== 더미 경로(지난 주 + dummy ON) =====
            var snap = WeeklyDummyDataGenerator.generate(memberId, weekMon);

            // 더미 주차가 비어 보이지 않도록 total이 0이면 기본 6개로 채움
            int displayTotal = (realTotalRoutines > 0 ? realTotalRoutines : DUMMY_MIN_TOTAL_ROUTINES);

            // 사용자의 카테고리 분포(없으면 라운드로빈으로 더미 분포 생성)
            Map<RoutineCategory, Long> routineCountByCat = myRoutines.stream()
                    .collect(Collectors.groupingBy(RoutineEntity::getCategory, Collectors.counting()));
            if (routineCountByCat.isEmpty()) {
                routineCountByCat = new EnumMap<>(RoutineCategory.class);
                RoutineCategory[] cats = RoutineCategory.values();
                for (int i = 0; i < displayTotal; i++) {
                    RoutineCategory c = cats[i % cats.length];
                    routineCountByCat.merge(c, 1L, Long::sum);
                }
            }

            dailyCompletion = new ArrayList<>();
            int overallDone = 0, overallTotal = 0;
            Map<RoutineCategory, int[]> catAgg = new EnumMap<>(RoutineCategory.class);

            for (int i = 0; i < 7; i++) {
                var day = snap.getDays().get(i);
                LocalDate d = day.getDate();
                boolean success = Boolean.TRUE.equals(day.getSuccess());

                int total = displayTotal;
                int done = (int) Math.round(total * (success ? 0.7 : 0.3));
                double rate = total == 0 ? 0.0 : round1(100.0 * done / total);

                dailyCompletion.add(DailyCompletionInfo.builder()
                        .date(d)
                        .done(done)
                        .total(total)
                        .rate(rate)
                        .mood(day.getMood()) // 직접 사용 (이미 EmotionType.name() 형태)
                        .isFuture(false)
                        .build());

                overallDone += done;
                overallTotal += total;

                // 카테고리별로 done 분배 (총합 보존)
                int remaining = done;
                for (var entry : routineCountByCat.entrySet()) {
                    RoutineCategory cat = entry.getKey();
                    int share = total == 0 ? 0
                            : (int) Math.floor((entry.getValue() * (long) done) / (double) total);
                    catAgg.computeIfAbsent(cat, k -> new int[2]);
                    catAgg.get(cat)[0] += share;                       // done
                    catAgg.get(cat)[1] += entry.getValue().intValue(); // total
                    remaining -= share;
                }
                if (remaining > 0 && !routineCountByCat.isEmpty()) {
                    var firstCat = routineCountByCat.keySet().stream()
                            .sorted(Comparator.comparing(Enum::name)).findFirst().get();
                    catAgg.get(firstCat)[0] += remaining;
                }
            }

            emotionDist = buildEmotionDistribution(dailyCompletion);

            Rate overall = Rate.builder()
                    .done(overallDone)
                    .total(overallTotal)
                    .rate(overallTotal == 0 ? 0.0 : round1(100.0 * overallDone / overallTotal))
                    .build();

            List<CategoryRate> categories = catAgg.entrySet().stream()
                    .map(e -> CategoryRate.builder()
                            .code(e.getKey().name())
                            .label(categoryLabel(e.getKey()))
                            .done(e.getValue()[0])
                            .total(e.getValue()[1])
                            .rate(e.getValue()[1] == 0 ? 0.0 : round1(100.0 * e.getValue()[0] / e.getValue()[1]))
                            .build())
                    .sorted(Comparator.comparing(CategoryRate::getCode))
                    .toList();

            metrics = MetricsInfo.builder()
                    .totalRoutines(displayTotal) // 더미 주차는 표시용 총 루틴 수를 사용
                    .overall(overall)
                    .categories(categories)
                    .build();
        }

        PeriodInfo period = PeriodInfo.builder()
                .weekStart(weekMon)
                .weekEnd(weekSun)
                .label(label)
                .isCurrentWeek(isCurrentWeek)
                .isComplete(isComplete)
                .nav(nav)
                .build();

        return WeeklyDashboardDto.builder()
                .period(period)
                .metrics(metrics)
                .emotionDistribution(emotionDist)
                .dailyCompletion(dailyCompletion)
                .build();
    }

    // ===== helpers =====

    private static String categoryLabel(RoutineCategory c) {
        String desc = c.getDescription();
        return (desc != null && !desc.isBlank()) ? desc : c.name();
    }

    private static Map<String, Integer> buildEmotionDistribution(List<DailyCompletionInfo> daily) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("LOW", 0);
        dist.put("NORMAL", 0);
        dist.put("GOOD", 0);
        dist.put("VERY_GOOD", 0);
        for (var d : daily) {
            if (d.isFuture()) continue;
            if (d.getMood() == null) continue;
            dist.computeIfPresent(d.getMood(), (k, v) -> v + 1);
        }
        return dist;
    }

    private static String ordinalOfWeekInMonth(LocalDate monday) {
        int weekOfMonth = (monday.getDayOfMonth() - 1) / 7 + 1;
        return String.valueOf(weekOfMonth);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
