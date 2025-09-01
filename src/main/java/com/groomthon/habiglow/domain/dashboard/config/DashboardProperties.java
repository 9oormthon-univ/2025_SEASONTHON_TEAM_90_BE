package com.groomthon.habiglow.domain.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 대시보드 관련 설정 Properties
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dashboard")
public class DashboardProperties {

    private WeekConfig week = new WeekConfig();
    private AiConfig ai = new AiConfig();
    private CacheConfig cache = new CacheConfig();

    @Getter
    @Setter
    public static class WeekConfig {
        /**
         * 분석 가능한 주차 조회 시 과거 몇 주까지 볼 것인지
         */
        private int lookbackWeeks = 12;

        /**
         * 더미 데이터 생성 시 포함할 기본 주차 수
         */
        private int dummyWeeks = 1;
    }

    @Getter
    @Setter
    public static class AiConfig {
        /**
         * OpenAI API 호출 시 temperature 값
         */
        private double temperature = 0.2;

        /**
         * 최대 토큰 수
         */
        private Integer maxTokens = 1500;

        /**
         * API 호출 재시도 횟수
         */
        private int retryAttempts = 2;

        /**
         * 재시도 간격 (초)
         */
        private int retryDelaySeconds = 3;
    }

    @Getter
    @Setter
    public static class CacheConfig {
        /**
         * 분석 결과 캐시 TTL (분)
         */
        private int insightCacheTtlMinutes = 60;

        /**
         * 주간 데이터 캐시 TTL (분)
         */
        private int weeklyDataCacheTtlMinutes = 30;

        /**
         * 캐시 사용 여부
         */
        private boolean enabled = true;
    }
}