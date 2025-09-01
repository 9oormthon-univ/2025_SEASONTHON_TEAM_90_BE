package com.groomthon.habiglow.domain.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dashboard.ai.prompt")
public class PromptProperties {
    /**
     * 시스템 프롬프트 전체 텍스트
     */
    private String systemPrompt;

    /**
     * 사용자 프롬프트 템플릿
     * 예) "아래 주간 스냅샷을 분석하세요.\n\n{snapshot}"
     *     -> {snapshot} 자리에 주간 입력 JSON이 바인딩됩니다.
     */
    private String userTemplate;
}
