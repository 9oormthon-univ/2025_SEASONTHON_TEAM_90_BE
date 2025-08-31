package com.groomthon.habiglow.domain.dashboard.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.entity.EmotionType;
import com.groomthon.habiglow.domain.daily.entity.PerformanceLevel;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.domain.routine.entity.RoutineEntity;
import com.groomthon.habiglow.domain.routine.repository.RoutineRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 분석 테스트를 위한 일주일치 더미 데이터 생성기
 * --spring.profiles.active=dev,dummy-data 로 실행
 */
@Slf4j
@Component
@Profile("dummy-data")
@RequiredArgsConstructor
public class WeeklyDummyDataGenerator implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final RoutineRepository routineRepository;
    private final DailyReflectionRepository reflectionRepository;
    private final DailyRoutineRepository dailyRoutineRepository;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== AI 분석용 더미 데이터 생성 시작 ===");

        // 테스트 사용자 찾기 (없으면 생성)
        MemberEntity testUser = findOrCreateTestUser();
        List<RoutineEntity> userRoutines = getUserRoutines(testUser);

        if (userRoutines.isEmpty()) {
            log.warn("사용자에게 루틴이 없습니다. 먼저 루틴을 생성해주세요.");
            return;
        }

        // 지난 주 데이터 생성 (월-일)
        LocalDate lastMonday = LocalDate.now()
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));

        generateWeeklyData(testUser, userRoutines, lastMonday);

        log.info("=== 더미 데이터 생성 완료 ===");
        log.info("생성된 기간: {} ~ {}", lastMonday, lastMonday.plusDays(6));
        log.info("AI 분석 API 테스트 가능합니다: POST /api/dashboard/insight/weekly");
    }

    private void generateWeeklyData(MemberEntity user, List<RoutineEntity> routines, LocalDate startDate) {
        for (int day = 0; day < 7; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            log.info("생성 중: {} ({})", currentDate, dayOfWeek);

            // 하루 회고 생성
            generateDailyReflection(user, currentDate, dayOfWeek);

            // 루틴 기록 생성
            generateRoutineRecords(user, routines, currentDate, dayOfWeek);
        }
    }

    private void generateDailyReflection(MemberEntity user, LocalDate date, DayOfWeek dayOfWeek) {
        ReflectionPattern pattern = getReflectionPattern(dayOfWeek);

        String content = selectRandom(pattern.reflectionTexts);
        EmotionType emotion = selectRandom(pattern.emotions);

        DailyReflectionEntity reflection = DailyReflectionEntity.create(user, content, emotion, date);
        reflectionRepository.save(reflection);

        log.debug("회고 생성: {} - {} ({})", date, content.substring(0, Math.min(20, content.length())), emotion);
    }

    private void generateRoutineRecords(MemberEntity user, List<RoutineEntity> routines,
                                        LocalDate date, DayOfWeek dayOfWeek) {
        PerformancePattern pattern = getPerformancePattern(dayOfWeek);

        int consecutiveDays = 0; // 실제로는 ConsecutiveDaysCalculator에서 계산되지만 더미로 설정

        for (RoutineEntity routine : routines) {
            PerformanceLevel performance = selectRandom(pattern.performances);

            // 연속일수 더미 계산
            if (performance == PerformanceLevel.FULL_SUCCESS) {
                consecutiveDays = random.nextInt(10) + 1; // 1-10일
            } else {
                consecutiveDays = 0;
            }

            DailyRoutineEntity routineRecord = DailyRoutineEntity.create(
                    routine, user, performance, date, consecutiveDays);

            dailyRoutineRepository.save(routineRecord);
        }

        log.debug("루틴 기록 생성: {} - {} 개 루틴", date, routines.size());
    }

    private ReflectionPattern getReflectionPattern(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.SOSO, EmotionType.HAPPY),
                    Arrays.asList(
                            "새로운 한 주 시작! 이번엔 꼭 계획대로 해보자",
                            "월요일이라 좀 무겁지만 의욕은 있어",
                            "주말이 너무 짧았다... 그래도 화이팅"
                    )
            );
            case TUESDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.SOSO, EmotionType.HAPPY),
                    Arrays.asList(
                            "어제보단 리듬이 좀 잡히는 느낌",
                            "화요일은 그럭저럭 할만한 것 같아",
                            "월요일 피로가 아직 남아있긴 해"
                    )
            );
            case WEDNESDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.SOSO, EmotionType.SAD),
                    Arrays.asList(
                            "벌써 수요일... 시간이 너무 빨라",
                            "중간 지점이라 그런지 좀 지쳐",
                            "수요일 고비만 넘으면 될 것 같은데"
                    )
            );
            case THURSDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.SOSO, EmotionType.HAPPY),
                    Arrays.asList(
                            "목요일이니까 곧 주말이겠네",
                            "컨디션 회복되는 느낌이야",
                            "어제보단 훨씬 나아졌어"
                    )
            );
            case FRIDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.HAPPY, EmotionType.SOSO),
                    Arrays.asList(
                            "드디어 금요일! 이번 주도 잘 버텼다",
                            "마무리 잘하고 주말 맞이하자",
                            "금요일이라 기분이 좋네"
                    )
            );
            case SATURDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.HAPPY, EmotionType.SOSO),
                    Arrays.asList(
                            "주말이라 여유롭긴 한데 루틴은 지켜야지",
                            "평일보다 느긋하게 보냈어",
                            "토요일엔 좀 더 재충전하는 시간을 가져야겠어"
                    )
            );
            case SUNDAY -> new ReflectionPattern(
                    Arrays.asList(EmotionType.SOSO, EmotionType.SAD),
                    Arrays.asList(
                            "내일부터 또 한 주가 시작이네...",
                            "일요일은 항상 아쉬워",
                            "다음 주 준비하면서 마음 다잡아보자"
                    )
            );
        };
    }

    private PerformancePattern getPerformancePattern(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> new PerformancePattern(
                    Arrays.asList(PerformanceLevel.PARTIAL_SUCCESS, PerformanceLevel.FULL_SUCCESS)
            );
            case TUESDAY, THURSDAY -> new PerformancePattern(
                    Arrays.asList(PerformanceLevel.FULL_SUCCESS, PerformanceLevel.PARTIAL_SUCCESS)
            );
            case WEDNESDAY -> new PerformancePattern(
                    Arrays.asList(PerformanceLevel.PARTIAL_SUCCESS, PerformanceLevel.NOT_PERFORMED)
            );
            case FRIDAY -> new PerformancePattern(
                    Arrays.asList(PerformanceLevel.FULL_SUCCESS, PerformanceLevel.PARTIAL_SUCCESS)
            );
            case SATURDAY, SUNDAY -> new PerformancePattern(
                    Arrays.asList(PerformanceLevel.PARTIAL_SUCCESS, PerformanceLevel.FULL_SUCCESS, PerformanceLevel.NOT_PERFORMED)
            );
        };
    }

    private MemberEntity findOrCreateTestUser() {
        return memberRepository.findByMemberEmail("test@habiglow.com")
                .orElseGet(() -> {
                    log.info("테스트 사용자 생성: test@habiglow.com");
                    MemberEntity testUser = MemberEntity.createSocialMember(
                            "test@habiglow.com", "테스트유저", null, null, null);
                    return memberRepository.save(testUser);
                });
    }

    private List<RoutineEntity> getUserRoutines(MemberEntity user) {
        return routineRepository.findByMember_Id(user.getId());
    }

    private <T> T selectRandom(List<T> items) {
        return items.get(random.nextInt(items.size()));
    }

    private static class ReflectionPattern {
        final List<EmotionType> emotions;
        final List<String> reflectionTexts;

        ReflectionPattern(List<EmotionType> emotions, List<String> reflectionTexts) {
            this.emotions = emotions;
            this.reflectionTexts = reflectionTexts;
        }
    }

    private static class PerformancePattern {
        final List<PerformanceLevel> performances;

        PerformancePattern(List<PerformanceLevel> performances) {
            this.performances = performances;
        }
    }
}