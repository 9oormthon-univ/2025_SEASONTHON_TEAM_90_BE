package com.groomthon.habiglow.domain.dashboard.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.groomthon.habiglow.domain.dashboard.config.DashboardProperties;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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

        if (activeProfiles.contains("dummy-data")) {
            return WeeklyDummyDataGenerator.generate(memberId, lastWeekMonday);
        }

        // TODO: 실제 데이터 수집 로직으로 교체
        // 현재는 최소 수정 원칙에 따라 더미와 동일 스키마로 빈 구조만 생성
        return WeeklyDummyDataGenerator.generate(memberId, lastWeekMonday);
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
}
