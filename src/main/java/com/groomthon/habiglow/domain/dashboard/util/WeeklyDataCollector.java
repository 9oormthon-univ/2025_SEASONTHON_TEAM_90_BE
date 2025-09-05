package com.groomthon.habiglow.domain.dashboard.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.groomthon.habiglow.domain.dashboard.config.DashboardProperties;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.EmotionType;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WeeklyDataCollector {

    private final DailyReflectionRepository reflectionRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final DashboardProperties dashboardProperties;

    //  스프링이 구성한 ObjectMapper 주입 (JavaTimeModule 포함)
    private final ObjectMapper objectMapper;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    /**
     * 지난주 데이터 스냅샷을 수집합니다.
     */
    public WeeklyAnalysisData collectLastWeekData(Long memberId) {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekMonday = thisMonday.minusWeeks(1);
        return collectWeekData(memberId, lastWeekMonday);
    }

    /**
     * 특정 주차 데이터 스냅샷을 수집합니다.
     */
    public WeeklyAnalysisData collectWeekData(Long memberId, LocalDate weekStart) {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        boolean isLastWeek = weekStart.equals(thisMonday.minusWeeks(1));
        boolean dummyOn = activeProfiles != null && activeProfiles.contains("dummy-data");

        // 더미 모드 + 지난주 = 더미 데이터
        if (dummyOn && isLastWeek) {
            return WeeklyDummyDataGenerator.generate(memberId, weekStart);
        }

        // 실제 DB에서 데이터 수집
        LocalDate weekEnd = weekStart.plusDays(6);
        
        List<DailyRoutineEntity> routineRecords = 
            dailyRoutineRepository.findByMemberIdAndPerformedDateBetween(
                memberId, weekStart, weekEnd);
        
        List<DailyReflectionEntity> reflections = 
            reflectionRepository.findByMemberIdAndReflectionDateBetweenOrderByReflectionDateAsc(
                memberId, weekStart, weekEnd);

        return convertToWeeklyAnalysisData(routineRecords, reflections, weekStart, memberId);
    }

    /**
     * 주어진 주간 데이터를 정규화된 JSON으로 직렬화합니다(순서 안정화).
     */
    public String toNormalizedSnapshotJson(WeeklyAnalysisData data) {
        try {
            // 전역 ObjectMapper를 변형하지 않도록 ObjectWriter로 옵션만 부여
            return objectMapper
                    .writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("주간 스냅샷 직렬화 실패", e);
        }
    }

    public int getLookbackWeeks() {
        return dashboardProperties.getLookbackWeeks();
    }

    public boolean includeDummyLastWeek() {
        return dashboardProperties.isIncludeDummyLastWeek();
    }

    /**
     * 실제 DB 데이터를 WeeklyAnalysisData로 변환
     */
    private WeeklyAnalysisData convertToWeeklyAnalysisData(
        List<DailyRoutineEntity> routineRecords,
        List<DailyReflectionEntity> reflections, 
        LocalDate weekStart,
        Long memberId) {
        
        LocalDate weekEnd = weekStart.plusDays(6);
        List<WeeklyAnalysisData.DayStat> days = new ArrayList<>();
        
        // 7일간 루프
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = weekStart.plusDays(i);
            
            // 해당 날짜의 루틴 기록들
            List<DailyRoutineEntity> dayRoutines = routineRecords.stream()
                .filter(r -> r.getPerformedDate().equals(currentDate))
                .toList();
            
            // 해당 날짜의 회고 기록
            Optional<DailyReflectionEntity> reflection = reflections.stream()
                .filter(r -> r.getReflectionDate().equals(currentDate))
                .findFirst();
            
            // 성공 여부 계산 (NOT_PERFORMED가 아닌 것의 비율)
            Boolean success = null;
            if (!dayRoutines.isEmpty()) {
                long performedCount = dayRoutines.stream()
                    .filter(r -> r.getPerformanceLevel() != PerformanceLevel.NOT_PERFORMED)
                    .count();
                success = performedCount > dayRoutines.size() / 2.0; // 과반수 이상 수행
            }
            
            // 감정 이모지 변환
            String mood = reflection
                .map(r -> emotionToEmoji(r.getEmotion()))
                .orElse(null);
            
            // 메모 수집
            String note = reflection
                .map(DailyReflectionEntity::getReflectionContent)
                .orElse(null);
            
            days.add(WeeklyAnalysisData.DayStat.builder()
                .date(currentDate)
                .success(success)
                .mood(mood)
                .note(note)
                .build());
        }
        
        return WeeklyAnalysisData.builder()
            .memberId(memberId)
            .weekStart(weekStart)
            .weekEnd(weekEnd)
            .days(days)
            .build();
    }

    /**
     * EmotionType을 이모지로 변환
     */
    private String emotionToEmoji(EmotionType emotion) {
        if (emotion == null) return null;
        return switch (emotion) {
            case VERY_GOOD -> "😊";
            case GOOD -> "🙂";
            case NORMAL -> "😐";
            case LOW -> "☹️";
        };
    }
}
