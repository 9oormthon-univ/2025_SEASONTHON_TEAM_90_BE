package com.groomthon.habiglow.domain.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.domain.dashboard.client.OpenAiClient;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyInsightDto;
import com.groomthon.habiglow.domain.dashboard.entity.WeeklyInsightEntity;
import com.groomthon.habiglow.domain.dashboard.repository.WeeklyInsightRepository;
import com.groomthon.habiglow.domain.dashboard.util.WeeklyDataCollector;
import com.groomthon.habiglow.domain.dashboard.util.WeeklyDummyDataGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeeklyInsightService {

    private static final String PROMPT_VERSION = "WKLY_V1";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WeeklyInsightRepository repo;
    private final WeeklyDataCollector dataCollector;
    private final OpenAiClient openAiClient;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public WeeklyInsightDto getOrCreate(Long memberId, LocalDate weekStart, boolean force) {
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate thisMon = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        boolean isLastWeek = weekStart.equals(thisMon.minusWeeks(1));
        boolean dummyOn = activeProfiles != null && activeProfiles.contains("dummy-data");

        // 1) 입력 스냅샷 구성 (정책: dummy on && 지난 주 → 더미 스냅샷, 그 외 → 기존 수집기)
        WeeklyAnalysisData data = (dummyOn && isLastWeek)
                ? WeeklyDummyDataGenerator.generate(memberId, weekStart)
                : collect(memberId, weekStart, weekEnd);

        String snapshot = dataCollector.toNormalizedSnapshotJson(data);
        String hash = sha256(snapshot);

        // 2) 기존 보고서 재사용
        if (!force) {
            Optional<WeeklyInsightEntity> ex = repo.findByMemberIdAndWeekStart(memberId, weekStart);
            if (ex.isPresent() && hash.equals(ex.get().getInputHash())) {
                return WeeklyInsightDto.from(ex.get());
            }
        }

        // 3) 분석 실행
        //    - dummy on && 지난 주: 더미 스냅샷으로 LLM 호출(시연용 출력 안정화 위해 엔티티 저장 후 재사용)
        //      (원하면 여기서 LLM 호출을 생략하고 고정 JSON을 넣는 방식으로 바꿔도 됨)
        OpenAiClient.AiResult ai = openAiClient.analyzeWeekly(snapshot, PROMPT_VERSION);

        // 4) upsert 저장
        WeeklyInsightEntity e = repo.findByMemberIdAndWeekStart(memberId, weekStart)
                .orElseGet(WeeklyInsightEntity::new);
        e.setMemberId(memberId);
        e.setWeekStart(weekStart);
        e.setWeekEnd(weekEnd);
        e.setInputSnapshotJson(snapshot);
        e.setInputHash(hash);
        e.setInsightJson(ai.resultJson());
        e.setModel(ai.model());
        e.setPromptVersion(ai.promptVersion());
        e.setPromptTokens(ai.promptTokens());
        e.setCompletionTokens(ai.completionTokens());
        repo.save(e);

        return WeeklyInsightDto.from(e);
    }

    public WeeklyInsightDto getLastWeekOrCreate(Long memberId, boolean force) {
        LocalDate thisMon = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekMon = thisMon.minusWeeks(1);
        return getOrCreate(memberId, lastWeekMon, force);
    }

    private WeeklyAnalysisData collect(Long memberId, LocalDate weekStart, LocalDate weekEnd) {
        // 범용 주차별 데이터 수집으로 수정
        return dataCollector.collectWeekData(memberId, weekStart);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("해시 계산 실패", e);
        }
    }
}
