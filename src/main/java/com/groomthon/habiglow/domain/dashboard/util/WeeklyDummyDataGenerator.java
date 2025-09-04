package com.groomthon.habiglow.domain.dashboard.util;

import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData;
import com.groomthon.habiglow.domain.dashboard.dto.WeeklyAnalysisData.DayStat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class WeeklyDummyDataGenerator {

    private WeeklyDummyDataGenerator() {}

    // 회고(노트) 샘플 문장들
    private static final List<String> SAMPLE_NOTES = List.of(
            "출근 전 20분 스트레칭 완료",
            "야근으로 피곤해서 루틴 건너뜀",
            "물 2L 달성, 집중 잘 됨",
            "늦게 자서 오전 컨디션 저하",
            "짧은 산책으로 기분 전환",
            "퇴근 후 헬스장 다녀옴",
            "주말 외출로 루틴 축소 진행"
    );

    // 이모지/성공 패턴(데모용, 월수금일 성공 / 화목토 실패)
    private static boolean successFor(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return (dow == DayOfWeek.MONDAY
                || dow == DayOfWeek.WEDNESDAY
                || dow == DayOfWeek.FRIDAY
                || dow == DayOfWeek.SUNDAY);
    }

    private static String moodFor(boolean success) {
        return success ? "🙂" : "😐";
    }

    /**
     * 주 시작일(월요일)을 기준으로 7일치 더미 스냅샷 생성.
     * 일부 날짜에만 note를 채워 '현실적인' 분포를 만듭니다(결과는 입력에 대해 결정적).
     */
    public static WeeklyAnalysisData generate(Long memberId, LocalDate weekStartMonday) {
        LocalDate weekEnd = weekStartMonday.plusDays(6);
        List<DayStat> days = new ArrayList<>(7);

        // 결정적 분포를 위해 고정 시드 역할(난수 없이 인덱스 계산)
        long seed = (memberId == null ? 0 : memberId) * 31L + weekStartMonday.toEpochDay();

        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStartMonday.plusDays(i);
            boolean success = successFor(d);
            String mood = moodFor(success);

            // 노트는 주 3~4회만 채우자: 요일/시드 기반으로 결정적으로 고르기
            boolean putNote = ((seed + i * 7) % 3 != 0); // 대략 66% 확률
            String note = null;
            if (putNote) {
                int idx = Math.floorMod((int) (seed + i), SAMPLE_NOTES.size());
                note = SAMPLE_NOTES.get(idx);
            }

            days.add(DayStat.builder()
                    .date(d)
                    .success(success)
                    .mood(mood)
                    .note(note)
                    .build());
        }

        return WeeklyAnalysisData.builder()
                .memberId(memberId)
                .weekStart(weekStartMonday)
                .weekEnd(weekEnd)
                .days(days)
                .build();
    }
}
