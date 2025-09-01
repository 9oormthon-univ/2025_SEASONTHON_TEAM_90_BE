package com.groomthon.habiglow.domain.dashboard.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groomthon.habiglow.domain.dashboard.config.PromptProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * OpenAI Chat Completions 간단 래퍼.
 * PromptProperties(systemPrompt, userTemplate)를 주입받아 사용.
 * (최소 수정: 공용 WebClient 빌더 사용, temperature 낮게.)
 */
@Service
@RequiredArgsConstructor
public class OpenAiClient {

    @Value("${openai.api.base-url}")
    private String baseUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model}")
    private String model;

    private final PromptProperties promptProperties;

    public AiResult analyzeWeekly(String snapshotJson, String promptVersion) {
        String systemPrompt = promptProperties.getSystemPrompt();
        String userPrompt = promptProperties.getUserTemplate()
                .replace("{snapshot}", snapshotJson);

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        ChatRequest req = ChatRequest.builder()
                .model(model)
                .messages(List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userPrompt)
                ))
                .temperature(0.2) // 안정성↑
                .build();

        ChatResponse resp = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        if (resp == null || resp.choices == null || resp.choices.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }

        String content = resp.choices.get(0).message.content;
        Integer promptTokens = resp.usage != null ? resp.usage.promptTokens : null;
        Integer completionTokens = resp.usage != null ? resp.usage.completionTokens : null;

        return new AiResult(content, model, promptVersion, promptTokens, completionTokens);
    }

    // ==== 내부 DTO들 (필요 최소) ====
    @Data @Builder
    private static class ChatRequest {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature;
    }

    @Data
    private static class ChatMessage {
        private final String role;
        private final String content;
    }

    @Data
    private static class ChatResponse {
        private List<Choice> choices;
        private Usage usage;

        @Data
        private static class Choice {
            private ChatMessage message;
        }

        @Data
        private static class Usage {
            @JsonProperty("prompt_tokens")
            private Integer promptTokens;
            @JsonProperty("completion_tokens")
            private Integer completionTokens;
        }
    }

    // 공개 결과
    public record AiResult(
            String resultJson,
            String model,
            String promptVersion,
            Integer promptTokens,
            Integer completionTokens
    ) {}
}
