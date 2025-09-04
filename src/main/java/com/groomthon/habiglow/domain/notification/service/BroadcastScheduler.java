package com.groomthon.habiglow.domain.notification.service;

import com.groomthon.habiglow.domain.notification.dto.response.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastScheduler {

    private final NotificationService notificationService;

    private static class TitleBody {
        private final String title;
        private final String body;
        private TitleBody(String title, String body) {
            this.title = title; this.body = body;
        }
        public String getTitle() { return title; }
        public String getBody() { return body; }
    }

    private TitleBody pickMessage(int hour) {
        if (hour < 12) return new TitleBody("좋은 아침이에요!", "오늘의 루틴으로 하루를 시작해보세요 ☀️");
        return new TitleBody("하루 수고하셨어요!", "오늘의 루틴을 정리해볼 시간이에요 🌙");
    }

    private Map<String,String> routeData(int hour) {
        return hour < 12
                ? Map.of("route","habiglow://routine","type","DAILY_REMINDER")
                : Map.of("route","habiglow://reflection","type","DAILY_REMINDER");
    }

    // 매일 KST 08:00, 22:00 전체 발송
    @Scheduled(cron = "0 0 8,22 * * *", zone = "Asia/Seoul")
    public void broadcastAtFixedTimes() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        int hour = now.getHour();
        TitleBody tb = pickMessage(hour);
        try {
            SendResult res = notificationService.sendBroadcast(tb.getTitle(), tb.getBody(), routeData(hour));
            log.info("[FCM] broadcast {}: success={}, failure={}", hour, res.getSuccess(), res.getFailure());
        } catch (Exception e) {
            log.error("[FCM] broadcast error at {}", hour, e);
        }
    }
}
