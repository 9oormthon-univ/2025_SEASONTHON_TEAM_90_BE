package com.groomthon.habiglow.domain.dashboard.service;

import com.groomthon.habiglow.domain.daily.entity.EmotionType;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.dashboard.dto.*;
import com.groomthon.habiglow.domain.dashboard.util.WeeklyDummyDataGenerator;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;

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

    public WeeklyDashboardDto getView(Long memberId, LocalDate weekStart) {
        LocalDate thisMon = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekMon = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekSun = weekMon.plusDays(6);

        boolean isCurrentWeek = weekMon.equals(thisMon);
        boolean isComplete = weekMon.isBefore(thisMon);
        boolean isFutureWeek = weekMon.isAfter(thisMon);

        NavInfo nav = NavInfo.builder()
                .hasPrev(true)
                .hasNext(!isCurrentWeek && !isFutureWeek)
                .prevWeekStart(weekMon.minusWeeks(1))
                .nextWeekStart(weekMon.plusWeeks(1))
                .build();

        String label = weekMon.getMonthValue() + "월 " + ordinalOfWeekInMonth(weekMon) + "째 주";

        // 사용자의 설정된 루틴들
        List<RoutineEntity> myRoutines = routineRepository.findByMember_Id(memberId);
        if (myRoutines == null) {
            myRoutines = new ArrayList<>();
        }
        int totalRoutines = myRoutines.size();

        // 실데이터 조회 (필드/메서드명 교정)
        List<DailyRoutineEntity> routineRecords =
                dailyRoutineRepository.findByMemberIdAndPerformedDateBetween(memberId, weekMon, weekSun);
        if (routineRecords == null) {
            routineRecords = new ArrayList<>();
        }

        List<DailyReflectionEntity> reflections =
                dailyReflectionRepository.findByMemberIdAndReflectionDateBetweenOrderByReflectionDateAsc(
                        memberId, weekMon, weekSun);
        if (reflections == null) {
            reflections = new ArrayList<>();
        }

        boolean dummyOn = activeProfiles != null && activeProfiles.contains("dummy-data");
        boolean isLastWeek = weekMon.equals(thisMon.minusWeeks(1));

        List<DailyCompletionInfo> dailyCompletion;
        Map<String, Integer> emotionDist;
        MetricsInfo metrics;

        if (!(dummyOn && isLastWeek)) {
            // === 지난 주가 아니거나,dummy 프로필이 꺼져있다면 ===
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
                        .map(r -> {
                            EmotionType e = r.getEmotion();
                            return e != null ? e.name() : null;
                        })
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
                    if (cat != null) {
                        catAgg.computeIfAbsent(cat, k -> new int[2]);
                        if (rec.getPerformanceLevel() != PerformanceLevel.NOT_PERFORMED) {
                            catAgg.get(cat)[0] += 1; // done
                        }
                        catAgg.get(cat)[1] += 1;     // total
                    }
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
                    .totalRoutines(totalRoutines)
                    .overall(overall)
                    .categories(categories)
                    .build();

        } else {
            // === 더미 기반 (지난 주 + dummy on + 실데이터 없음) ===
            var snap = WeeklyDummyDataGenerator.generate(memberId, weekMon);

            dailyCompletion = new ArrayList<>();
            int overallDone = 0, overallTotal = 0;

            // 사용자가 설정한 루틴의 카테고리 구성
            Map<RoutineCategory, Long> routineCountByCat = myRoutines.stream()
                    .collect(Collectors.groupingBy(RoutineEntity::getCategory, Collectors.counting()));

            Map<RoutineCategory, int[]> catAgg = new EnumMap<>(RoutineCategory.class);

            for (int i = 0; i < 7; i++) {
                var day = snap.getDays().get(i);
                LocalDate d = day.getDate();
                boolean success = Boolean.TRUE.equals(day.getSuccess());
                int total = totalRoutines;
                int done = Math.max(0, (int) Math.round(total * (success ? 0.7 : 0.3)));
                double rate = total == 0 ? 0.0 : round1(100.0 * done / total);

                dailyCompletion.add(DailyCompletionInfo.builder()
                        .date(d)
                        .done(done)
                        .total(total)
                        .rate(rate)
                        .mood(emojiToMoodName(day.getMood()))
                        .isFuture(false)
                        .build());

                overallDone += done;
                overallTotal += total;

                int remaining = done;
                for (var entry : routineCountByCat.entrySet()) {
                    RoutineCategory cat = entry.getKey();
                    if (cat != null) {
                        int share = totalRoutines == 0 ? 0
                                : Math.max(0, (int) Math.floor((entry.getValue() * (long) done) / (double) totalRoutines));
                        catAgg.computeIfAbsent(cat, k -> new int[2]);
                        catAgg.get(cat)[0] += share;                       // done
                        catAgg.get(cat)[1] += entry.getValue().intValue(); // total
                        remaining = Math.max(0, remaining - share);
                    }
                }
                if (remaining > 0 && !routineCountByCat.isEmpty()) {
                    var firstCatOpt = routineCountByCat.keySet().stream()
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparing(Enum::name))
                            .findFirst();
                    if (firstCatOpt.isPresent()) {
                        catAgg.get(firstCatOpt.get())[0] += remaining;
                    }
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
                    .totalRoutines(totalRoutines)
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

    private static String emojiToMoodName(String emoji) {
        if (emoji == null) return null;
        return switch (emoji) {
            case "🙂", "😊", "😀" -> "HAPPY";
            case "😐", "😶" -> "SOSO";
            case "☹️", "🙁", "😢" -> "SAD";
            case "😡", "😠" -> "MAD";
            default -> "SOSO";
        };
    }

    private static Map<String, Integer> buildEmotionDistribution(List<DailyCompletionInfo> daily) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("HAPPY", 0);
        dist.put("SOSO", 0);
        dist.put("SAD", 0);
        dist.put("MAD", 0);
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
