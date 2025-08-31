package com.groomthon.habiglow.domain.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * OpenAI API 설정 Properties
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai.api")
public class OpenAiProperties {

    private String key;
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private String timeout = "30s";
    private int maxRetries = 3;

    public long getTimeoutMillis() {
        // "30s" -> 30000ms 변환
        if (timeout.endsWith("s")) {
            return Long.parseLong(timeout.substring(0, timeout.length() - 1)) * 1000;
        }
        return 30000; // 기본값
    }
}