package com.groomthon.habiglow.domain.dashboard.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groomthon.habiglow.domain.dashboard.config.PromptProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.List;

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

    // 한 번만 만들어 재사용
    private WebClient client;

    @PostConstruct
    void init() {
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AiResult analyzeWeekly(String snapshotJson, String promptVersion) {
        String systemPrompt = promptProperties.getSystemPrompt();
        String userPrompt   = promptProperties.getUserTemplate().replace("{snapshot}", snapshotJson);

        ChatRequest req = new ChatRequest(
                model,
                List.of(new ChatMessage("system", systemPrompt),
                        new ChatMessage("user",   userPrompt)),
                0.2 // 안정성 유지
        );

        ChatResponse resp = client.post()
                .uri("/chat/completions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }

        String content = resp.choices().get(0).message().content();
        Integer promptTokens     = resp.usage() != null ? resp.usage().promptTokens() : null;
        Integer completionTokens = resp.usage() != null ? resp.usage().completionTokens() : null;

        return new AiResult(content, model, promptVersion, promptTokens, completionTokens);
    }

    // ==== 내부 DTO(record) ====
    public record ChatRequest(String model, List<ChatMessage> messages, Double temperature) {}
    public record ChatMessage(String role, String content) {}
    public record ChatResponse(List<Choice> choices, Usage usage) {}
    public record Choice(ChatMessage message) {}
    public record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                        @JsonProperty("completion_tokens") Integer completionTokens) {}

    // 공개 결과
    public record AiResult(String resultJson, String model, String promptVersion,
                           Integer promptTokens, Integer completionTokens) {}
}
