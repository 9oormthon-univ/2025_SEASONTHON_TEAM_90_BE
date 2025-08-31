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
import com.groomthon.habiglow.domain.dashboard.config.OpenAiProperties;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.response.WeeklyInsightResponse;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI Chat Completions API 클라이언트
 * 기획서의 프롬프트와 JSON 스키마를 적용하여 주간 분석 수행
 */
@Slf4j
@Service
public class OpenAiClient {

    private final WebClient webClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    // 기획서에서 정의한 시스템 프롬프트
    private static final String SYSTEM_PROMPT = """
        You are HabiGlow Weekly Analyst.
        
        Purpose
        - Summarize Mon–Sun habit successes/failures and mood changes.
        - Provide exactly ONE empathy sentence and ONE encouragement sentence.
        - No blame; describe failures as patterns.
        
        Style (Korean, 존댓말)
        - Always write in Korean (formal, 존댓말).
        - Be concise and concrete (numbers, counts).
        - Use emojis only in mood fields; other texts: 0–1 emoji at most.
        
        Output rules
        - Must follow the provided JSON schema exactly (no extra fields).
        - Length guide: weekly_summary ≤ 80 chars, empathy ≤ 80 chars, encouragement ≤ 80 chars.
        """;

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();

        log.info("OpenAI 클라이언트 초기화 완료 - Model: {}, BaseURL: {}",
                properties.getModel(), properties.getBaseUrl());
    }

    /**
     * 주간 데이터를 분석하여 인사이트 생성
     */
    public WeeklyInsightResponse generateWeeklyInsight(WeeklyAnalysisData weeklyData) {
        try {
            String userMessage = String.format("Here is the weekly data in JSON:\n\n%s\n\nFollow the schema strictly.",
                    objectMapper.writeValueAsString(weeklyData));

            Map<String, Object> requestBody = buildChatCompletionRequest(userMessage);

            log.info("OpenAI API 호출 시작 - 주간 분석 ({})", weeklyData.getWeekStart());

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                    .block();

            return parseResponse(response);

        } catch (WebClientResponseException e) {
            log.error("OpenAI API 호출 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ErrorCode.AI_ANALYSIS_FAILED);
        } catch (Exception e) {
            log.error("AI 분석 중 예외 발생", e);
            throw new BaseException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    private Map<String, Object> buildChatCompletionRequest(String userMessage) {
        // JSON Schema 정의 (기획서 스키마)
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
                        "good_points", "empathy", "encouragement"
                ),
                "additionalProperties", false
        );

        return Map.of(
                "model", properties.getModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
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

    private WeeklyInsightResponse parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");

            if (choices.isEmpty()) {
                throw new RuntimeException("OpenAI 응답에 choices가 없습니다");
            }

            String content = choices.get(0).path("message").path("content").asText();

            log.info("AI 분석 완료 - 응답 길이: {} chars", content.length());
            log.debug("AI 응답 내용: {}", content);

            return objectMapper.readValue(content, WeeklyInsightResponse.class);

        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패", e);
            throw new BaseException(ErrorCode.AI_RESPONSE_PARSE_FAILED);
        }
    }
}