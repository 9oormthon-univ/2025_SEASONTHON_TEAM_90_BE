package com.groomthon.habiglow.domain.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.domain.dashboard.config.DashboardProperties;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity;
import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity.AnalysisType;
import com.groomthon.habiglow.domain.dashboard.repository.WeeklyInsightRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyInsightService {

    private final WeeklyDataCollector dataCollector;
    private final OpenAiClient openAiClient;
    private final WeeklyInsightRepository insightRepository;
    private final DashboardProperties dashboardProperties;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 지난주 분석 - DB에 저장된 결과가 있으면 반환, 없으면 생성 후 저장
     */
    @Transactional
    public WeeklyInsightResponse generateLastWeekInsight(Long memberId) {
        LocalDate lastWeekMonday = getLastWeekMonday();

        return getOrCreateInsight(memberId, lastWeekMonday, AnalysisType.LAST_WEEK,
                () -> dataCollector.collectLastWeekData(memberId));
    }

    /**
     * 특정 주차 분석 - DB에 저장된 결과가 있으면 반환, 없으면 생성 후 저장
     */
    @Transactional
    public WeeklyInsightResponse generateSpecificWeekInsight(Long memberId, LocalDate weekStart) {
        return getOrCreateInsight(memberId, weekStart, AnalysisType.SPECIFIC_WEEK,
                () -> dataCollector.collectSpecificWeekData(memberId, weekStart));
    }

    /**
     * 이번주 진행상황 분석 - 매번 새로 생성 (진행 중이므로 캐시하지 않음)
     */
    @Transactional
    public WeeklyInsightResponse generateThisWeekInsight(Long memberId) {
        LocalDate thisWeekMonday = getThisWeekMonday();

        // 이번주는 매번 새로 분석 (기존 결과가 있으면 삭제 후 재생성)
        insightRepository.findByMemberIdAndWeekStartDateAndAnalysisType(
                        memberId, thisWeekMonday, AnalysisType.CURRENT_WEEK)
                .ifPresent(insightRepository::delete);

        WeeklyAnalysisData data = dataCollector.collectThisWeekData(memberId);
        validateAnalysisData(data);

        WeeklyInsightResponse aiResponse = openAiClient.generateWeeklyInsight(data);
        WeeklyInsightEntity entity = saveInsightToDb(memberId, thisWeekMonday, aiResponse, AnalysisType.CURRENT_WEEK);

        log.info(" 이번주 분석 완료 및 저장 - 사용자: {}, 주차: {}", memberId, thisWeekMonday);
        return convertToResponse(entity);
    }

    /**
     * 지난주 완료 여부 확인
     */
    public boolean isLastWeekCompleted(Long memberId) {
        LocalDate lastWeekMonday = getLastWeekMonday();

        // DB에 저장된 분석 결과가 있으면 완료로 간주
        if (insightRepository.existsByMemberIdAndWeekStartDate(memberId, lastWeekMonday)) {
            return true;
        }

        // 실제 데이터 존재 여부로 판단
        return dataCollector.isLastWeekCompleted(memberId);
    }

    /**
     * 분석 가능한 주차 목록 조회 (DB 저장된 분석 결과 + 실데이터 있는 주차)
     */
    @Cacheable(value = "availableWeeks", key = "#memberId")
    public List<String> getAvailableWeeks(Long memberId) {
        // DB에 저장된 분석 결과가 있는 주차들
        List<LocalDate> savedWeeks = insightRepository
                .findDistinctWeekStartDatesByMemberIdOrderByWeekStartDateDesc(memberId);

        // 실데이터는 있지만 분석은 안 된 주차들 (최근 몇 주만 체크)
        List<LocalDate> dataOnlyWeeks = dataCollector.getAvailableWeeks(memberId)
                .stream()
                .map(weekRange -> LocalDate.parse(weekRange.substring(0, 10), DF))
                .filter(date -> !savedWeeks.contains(date))
                .collect(Collectors.toList());

        // 통합 후 정렬
        savedWeeks.addAll(dataOnlyWeeks);

        return savedWeeks.stream()
                .distinct()
                .sorted((a, b) -> b.compareTo(a)) // 최신순
                .limit(dashboardProperties.getWeek().getLookbackWeeks())
                .map(this::toWeekRange)
                .collect(Collectors.toList());
    }

    /**
     * 주차별 실데이터 존재 여부 확인 (컨트롤러용)
     */
    public boolean hasRealWeekData(Long memberId, LocalDate weekStart) {
        return dataCollector.hasRealWeekData(memberId, weekStart);
    }

    /**
     * 핵심 메서드: DB에서 조회 → 없으면 AI 분석 후 저장
     */
    private WeeklyInsightResponse getOrCreateInsight(Long memberId, LocalDate weekStart,
                                                     AnalysisType analysisType,
                                                     java.util.function.Supplier<WeeklyAnalysisData> dataSupplier) {

        // 1. DB에서 기존 분석 결과 조회
        Optional<WeeklyInsightEntity> existingInsight =
                insightRepository.findByMemberIdAndWeekStartDateAndAnalysisType(memberId, weekStart, analysisType);

        if (existingInsight.isPresent()) {
            log.info(" 기존 분석 결과 반환 - 사용자: {}, 주차: {}, 타입: {}",
                    memberId, weekStart, analysisType);
            return convertToResponse(existingInsight.get());
        }

        // 2. 기존 결과가 없으면 새로 분석
        log.info(" 새로운 AI 분석 시작 - 사용자: {}, 주차: {}, 타입: {}",
                memberId, weekStart, analysisType);

        WeeklyAnalysisData data = dataSupplier.get();
        validateAnalysisData(data);

        WeeklyInsightResponse aiResponse = openAiClient.generateWeeklyInsight(data);
        WeeklyInsightEntity savedEntity = saveInsightToDb(memberId, weekStart, aiResponse, analysisType);

        log.info(" AI 분석 완료 및 저장 - 사용자: {}, 주차: {}", memberId, weekStart);
        return convertToResponse(savedEntity);
    }

    /**
     * AI 분석 결과를 DB에 저장
     */
    private WeeklyInsightEntity saveInsightToDb(Long memberId, LocalDate weekStart,
                                                WeeklyInsightResponse response, AnalysisType analysisType) {
        try {
            LocalDate weekEnd = weekStart.plusDays(6);

            WeeklyInsightEntity entity = WeeklyInsightEntity.builder()
                    .memberId(memberId)
                    .weekStartDate(weekStart)
                    .weekEndDate(weekEnd)
                    .weekRange(response.getWeekRange())
                    .moodDaily(objectMapper.writeValueAsString(response.getMoodDaily()))
                    .moodTrend(response.getMoodTrend())
                    .weeklySummary(response.getWeeklySummary())
                    .goodPoints(objectMapper.writeValueAsString(response.getGoodPoints()))
                    .failurePatterns(objectMapper.writeValueAsString(response.getFailurePatterns()))
                    .empathy(response.getEmpathy())
                    .encouragement(response.getEncouragement())
                    .analysisType(analysisType)
                    .build();

            return insightRepository.save(entity);

        } catch (JsonProcessingException e) {
            log.error("❌ AI 분석 결과 JSON 직렬화 실패", e);
            throw new BaseException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private WeeklyInsightResponse convertToResponse(WeeklyInsightEntity entity) {
        try {
            return WeeklyInsightResponse.builder()
                    .weekRange(entity.getWeekRange())
                    .moodDaily(objectMapper.readValue(entity.getMoodDaily(), List.class))
                    .moodTrend(entity.getMoodTrend())
                    .weeklySummary(entity.getWeeklySummary())
                    .goodPoints(objectMapper.readValue(entity.getGoodPoints(), List.class))
                    .failurePatterns(objectMapper.readValue(entity.getFailurePatterns(), List.class))
                    .empathy(entity.getEmpathy())
                    .encouragement(entity.getEncouragement())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("❌ 저장된 분석 결과 JSON 역직렬화 실패", e);
            throw new BaseException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    private void validateAnalysisData(WeeklyAnalysisData data) {
        if (data == null || data.getWeekStart() == null || data.getWeekEnd() == null) {
            throw new IllegalStateException("주간 데이터가 유효하지 않습니다.");
        }
    }

    private LocalDate getLastWeekMonday() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1);
    }

    private LocalDate getThisWeekMonday() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String toWeekRange(LocalDate monday) {
        return DF.format(monday) + " ~ " + DF.format(monday.plusDays(6));
    }
}