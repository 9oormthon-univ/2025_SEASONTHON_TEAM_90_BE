package com.groomthon.habiglow.domain.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dashboard.weekly")
public class DashboardProperties {
    /**
     * 대시보드 조회 시 과거 몇 주까지 조회할지
     */
    private int lookbackWeeks = 12;

    /**
     * 목록에 지난주 더미 엔트리(시연용)를 포함할지
     */
    private boolean includeDummyLastWeek = true;
}
