package com.groomthon.habiglow.domain.dashboard.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.domain.dashboard.config.DashboardProperties;
import com.groomthon.habiglow.domain.dashboard.config.OpenAiProperties;
import com.groomthon.habiglow.domain.dashboard.config.PromptProperties;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 수동 재시도 로직이 적용된 OpenAI Chat Completions API 클라이언트
 * - @Retryable 대신 수동 재시도 로직 사용 (더 안정적)
 * - 프롬프트 외부화 적용
 * - 상세한 에러 처리 및 로깅
 */
@Slf4j
@Service
public class OpenAiClient {

    private final WebClient webClient;
    private final OpenAiProperties openAiProperties;
    private final DashboardProperties dashboardProperties;
    private final PromptProperties promptProperties;
    private final ObjectMapper objectMapper;

    public OpenAiClient(OpenAiProperties openAiProperties,
                        DashboardProperties dashboardProperties,
                        PromptProperties promptProperties) {
        this.openAiProperties = openAiProperties;
        this.dashboardProperties = dashboardProperties;
        this.promptProperties = promptProperties;
        this.objectMapper = new ObjectMapper();

        this.webClient = WebClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "HabiGlow-AI-Client/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();

        log.info("OpenAI 클라이언트 초기화 완료 - Model: {}, BaseURL: {}, Temperature: {}",
                openAiProperties.getModel(), openAiProperties.getBaseUrl(),
                dashboardProperties.getAi().getTemperature());
    }

    /**
     * 주간 데이터를 분석하여 인사이트 생성 (수동 재시도 로직)
     */
    public WeeklyInsightResponse generateWeeklyInsight(WeeklyAnalysisData weeklyData) {
        int maxAttempts = dashboardProperties.getAi().getRetryAttempts();
        int delaySeconds = dashboardProperties.getAi().getRetryDelaySeconds();

        log.info("AI 분석 시작 - 주차: {} ~ {}, 최대 재시도: {}회",
                weeklyData.getWeekStart(), weeklyData.getWeekEnd(), maxAttempts);

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("API 호출 시도: {}/{}", attempt, maxAttempts);
                return callOpenAiApi(weeklyData);

            } catch (WebClientResponseException e) {
                lastException = e;

                log.error("OpenAI API 호출 실패 (시도 {}/{}) - Status: {}, Body: {}",
                        attempt, maxAttempts, e.getStatusCode(), e.getResponseBodyAsString());

                // 4xx 에러는 재시도하지 않음 (클라이언트 오류)
                if (e.getStatusCode().is4xxClientError()) {
                    log.error("클라이언트 오류로 재시도 중단 - Status: {}", e.getStatusCode());
                    throw new BaseException(ErrorCode.AI_API_CLIENT_ERROR);
                }

                // 마지막 시도였다면 예외 던지기
                if (attempt == maxAttempts) {
                    log.error("모든 재시도 실패 - 최종 시도: {}/{}", attempt, maxAttempts);
                    break;
                }

                // 재시도 전 대기
                log.warn("API 호출 실패, {}초 후 재시도 예정 ({}/{})",
                        delaySeconds, attempt, maxAttempts);
                waitBeforeRetry(delaySeconds);

            } catch (Exception e) {
                lastException = e;

                log.error("예상치 못한 오류 발생 (시도 {}/{}) - 원인: {}",
                        attempt, maxAttempts, e.getMessage(), e);

                // 마지막 시도가 아니면 재시도
                if (attempt < maxAttempts) {
                    log.warn("{}초 후 재시도 예정 ({}/{})", delaySeconds, attempt, maxAttempts);
                    waitBeforeRetry(delaySeconds);
                } else {
                    log.error("모든 재시도 실패 - 최종 시도: {}/{}", attempt, maxAttempts);
                    break;
                }
            }
        }

        // 모든 재시도 실패 시
        log.error("AI 분석 최종 실패 - 마지막 오류: {}",
                lastException != null ? lastException.getMessage() : "알 수 없는 오류");
        throw new BaseException(ErrorCode.AI_ANALYSIS_FAILED);
    }

    /**
     * 실제 OpenAI API 호출
     */
    private WeeklyInsightResponse callOpenAiApi(WeeklyAnalysisData weeklyData) throws Exception {
        String userMessage = buildUserMessage(weeklyData);
        Map<String, Object> requestBody = buildChatCompletionRequest(userMessage);

        log.debug("요청 데이터 크기: {} bytes", objectMapper.writeValueAsString(requestBody).length());

        String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(openAiProperties.getTimeoutMillis()))
                .block();

        WeeklyInsightResponse result = parseResponse(response);
        log.info("AI 분석 성공 완료");
        return result;
    }

    /**
     * 재시도 전 대기
     */
    private void waitBeforeRetry(int delaySeconds) {
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("재시도 대기 중 인터럽트 발생");
            throw new BaseException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    /**
     * 사용자 메시지 템플릿 기반으로 메시지 구성
     */
    private String buildUserMessage(WeeklyAnalysisData weeklyData) throws Exception {
        String jsonData = objectMapper.writeValueAsString(weeklyData);
        String weekRange = weeklyData.getWeekStart() + " ~ " + weeklyData.getWeekEnd();

        return String.format(promptProperties.getUserTemplate(), weekRange, jsonData);
    }

    /**
     * Chat Completion 요청 본문 구성
     */
    private Map<String, Object> buildChatCompletionRequest(String userMessage) {
        // JSON Schema 정의 (기획서 기준)
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "week_range", Map.of("type", "string", "description", "YYYY-MM-DD ~ YYYY-MM-DD"),
                        "mood_daily", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string", "enum", List.of("😀","🙂","😐","☁️","😞")),
                                "minItems", 7,
                                "maxItems", 7
                        ),
                        "mood_trend", Map.of("type", "string", "enum", List.of("상승","하락","안정")),
                        "weekly_summary", Map.of("type", "string", "maxLength", 80),
                        "good_points", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string", "maxLength", 60),
                                "minItems", 1,
                                "maxItems", 3
                        ),
                        "failure_patterns", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string", "maxLength", 60),
                                "minItems", 0,
                                "maxItems", 3
                        ),
                        "empathy", Map.of("type", "string", "maxLength", 80),
                        "encouragement", Map.of("type", "string", "maxLength", 80)
                ),
                "required", List.of(
                        "week_range", "mood_daily", "mood_trend", "weekly_summary",
                        "good_points","failure_patterns", "empathy", "encouragement"
                ),
                "additionalProperties", false
        );

        return Map.of(
                "model", openAiProperties.getModel(),
                "temperature", dashboardProperties.getAi().getTemperature(),
                "max_tokens", dashboardProperties.getAi().getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", promptProperties.getSystemPrompt()),
                        Map.of("role", "user", "content", userMessage)
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "HabiGlowWeeklyReport",
                                "strict", true,
                                "schema", schema
                        )
                )
        );
    }

    /**
     * API 응답 파싱
     */
    private WeeklyInsightResponse parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // usage 정보 로깅
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                log.info("토큰 사용량 - Prompt: {}, Completion: {}, Total: {}",
                        usage.path("prompt_tokens").asInt(),
                        usage.path("completion_tokens").asInt(),
                        usage.path("total_tokens").asInt());
            }

            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                log.error("OpenAI 응답에 choices가 없음 - Raw Response: {}", response);
                throw new RuntimeException("OpenAI 응답에 choices가 없습니다");
            }

            String content = choices.get(0).path("message").path("content").asText();

            // finish_reason 체크
            String finishReason = choices.get(0).path("finish_reason").asText();
            if (!"stop".equals(finishReason)) {
                log.warn("응답이 정상적으로 완료되지 않음 - finish_reason: {}", finishReason);
            }

            log.info("AI 응답 파싱 완료 - 응답 길이: {} chars", content.length());
            log.debug("AI 응답 내용: {}", content);

            WeeklyInsightResponse result = objectMapper.readValue(content, WeeklyInsightResponse.class);
            validateResponse(result);

            return result;

        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패 - Raw Response: {}", response, e);
            throw new BaseException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }

    /**
     * AI 응답 결과 검증
     */
    private void validateResponse(WeeklyInsightResponse response) {
        if (response.getMoodDaily() == null || response.getMoodDaily().size() != 7) {
            throw new IllegalArgumentException("mood_daily는 정확히 7개의 요소를 가져야 합니다.");
        }

        if (response.getWeeklySummary() == null || response.getWeeklySummary().trim().isEmpty()) {
            throw new IllegalArgumentException("weekly_summary는 필수입니다.");
        }

        if (response.getGoodPoints() == null || response.getGoodPoints().isEmpty()) {
            throw new IllegalArgumentException("good_points는 최소 1개 이상이어야 합니다.");
        }

        log.debug("AI 응답 검증 완료");
    }
}