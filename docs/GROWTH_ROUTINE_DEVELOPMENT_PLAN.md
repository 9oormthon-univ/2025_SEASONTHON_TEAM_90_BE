# 성장 루틴 자동 증가 기능 개발 계획서

> **작성일**: 2025-01-31  
> **담당자**: 개발팀  
> **예상 개발 기간**: 3일  
> **우선순위**: High  

---

## 🎯 프로젝트 개요

### 목표
사용자가 로그인할 때 성장 주기를 완료한 루틴을 자동으로 감지하고, 사용자 확인 후 목표치를 자동 증가시키는 시스템 구축

### 배경
- 점진적 과부하(Progressive Overload) 원리 적용
- 사용자 습관 형성의 지속적인 동기부여 제공
- 수동 목표 관리의 번거로움 해소

### 핵심 기능
1. **성장 주기 완료 자동 감지**: 로그인 시 전날까지 성장 조건 충족 루틴 확인
2. **사용자 확인 후 증가**: 강제가 아닌 사용자 선택에 의한 목표치 증가  
3. **성장 주기 리셋**: 성장을 거부할 때 주기를 초기화하여 새로운 도전 기회 제공
4. **명확한 피드백**: 증가 전후 목표 비교 및 성공 메시지 제공

---

## 🏗️ 시스템 아키텍처

### 기존 시스템 연동
```
RoutineEntity (성장 설정) ←→ DailyRoutineEntity (연속 성공일)
       ↓
성장 주기 완료 감지 로직
       ↓  
클라이언트 알림 → 사용자 확인 → 목표치 증가
```

### 신규 컴포넌트
- `RoutineGrowthCheckService`: 성장 주기 완료 감지
- `GrowthCheckResponse`: 성장 가능한 루틴 정보 전달
- `IncreaseTargetResponse`: 목표 증가 결과 전달
- `ResetGrowthCycleResponse`: 성장 주기 리셋 결과 전달
- `GrowthSettings.currentCycleDays`: 현재 주기 내 연속일 관리

---

## 📋 상세 개발 계획

## **1단계: 기반 작업 (Day 1 - 오전)**

### 1.1 Response DTO 클래스 생성

#### 📁 **파일**: `GrowthCheckResponse.java`
```java
@Getter
@Builder
@AllArgsConstructor
public class GrowthCheckResponse {
    private List<GrowthReadyRoutineResponse> growthReadyRoutines;
    private Integer totalGrowthReadyCount;
}
```

#### 📁 **파일**: `GrowthReadyRoutineResponse.java`
```java
@Getter
@Builder 
@AllArgsConstructor
public class GrowthReadyRoutineResponse {
    private Long routineId;
    private String title;
    private RoutineCategory category;
    private TargetType targetType;
    private Integer currentTarget;
    private Integer nextTarget;
    private Integer increment;
    private Integer completedCycleDays;
    private Integer consecutiveDays;
    private Integer currentCycleDays;  // 🆕 현재 주기 내 연속일
    private LocalDate lastPerformedDate;
    
    // RoutineEntity와 DailyRoutineEntity로부터 생성하는 팩토리 메서드
    public static GrowthReadyRoutineResponse from(RoutineEntity routine, DailyRoutineEntity lastRecord) {
        return GrowthReadyRoutineResponse.builder()
            .routineId(routine.getRoutineId())
            .title(routine.getTitle())
            .category(routine.getCategory())
            .targetType(routine.getTargetType())
            .currentTarget(routine.getTargetValue())
            .nextTarget(routine.getTargetValue() + routine.getTargetIncrement())
            .increment(routine.getTargetIncrement())
            .completedCycleDays(routine.getGrowthCycleDays())
            .consecutiveDays(lastRecord.getConsecutiveDays())
            .currentCycleDays(routine.getCurrentCycleDays())  // 🆕
            .lastPerformedDate(lastRecord.getPerformedDate())
            .build();
    }
}
```

#### 📁 **파일**: `IncreaseTargetResponse.java`
```java
@Getter
@Builder
@AllArgsConstructor
public class IncreaseTargetResponse {
    private Long routineId;
    private String title;
    private Integer previousTarget;
    private Integer newTarget;
    private Integer increment;
    private TargetType targetType;
    private String message;
    
    public static IncreaseTargetResponse from(RoutineEntity routine, Integer previousTarget) {
        String targetUnit = routine.getTargetType().getUnit();
        String message = String.format("목표가 %d%s에서 %d%s로 증가되었습니다!", 
            previousTarget, targetUnit, routine.getTargetValue(), targetUnit);
            
        return IncreaseTargetResponse.builder()
            .routineId(routine.getRoutineId())
            .title(routine.getTitle())
            .previousTarget(previousTarget)
            .newTarget(routine.getTargetValue())
            .increment(routine.getTargetIncrement())
            .targetType(routine.getTargetType())
            .message(message)
            .build();
    }
}
```

### 1.2 TargetType enum 개선

#### 📁 **파일 수정**: `TargetType.java`
```java
public enum TargetType {
    DATE("날짜", "일"),    // 🔄 실제 구현된 값
    NUMBER("숫자", "개");  // 🔄 실제 구현된 값
    
    private final String description;
    private final String unit;
    
    TargetType(String description, String unit) {
        this.description = description;
        this.unit = unit;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getUnit() {
        return unit;
    }
}
```

### 1.3 Repository 쿼리 메서드 추가

#### 📁 **파일 수정**: `RoutineRepository.java`
```java
// 성장 모드 활성화된 루틴 조회
@Query("SELECT r FROM RoutineEntity r WHERE r.member.id = :memberId AND r.growthSettings.isGrowthMode = true")
List<RoutineEntity> findGrowthEnabledRoutinesByMemberId(@Param("memberId") Long memberId);
```

#### 📁 **파일 수정**: `DailyRoutineRepository.java`
```java
// 특정 날짜의 FULL_SUCCESS 기록 조회
@Query("SELECT dr FROM DailyRoutineEntity dr WHERE dr.routine.routineId = :routineId " +
       "AND dr.member.id = :memberId AND dr.performedDate = :date " +
       "AND dr.performanceLevel = 'FULL_SUCCESS'")
Optional<DailyRoutineEntity> findSuccessRecordByRoutineAndMemberAndDate(
    @Param("routineId") Long routineId,
    @Param("memberId") Long memberId, 
    @Param("date") LocalDate date);

// 성장 확인을 위한 최적화된 배치 쿼리
@Query("SELECT dr FROM DailyRoutineEntity dr WHERE dr.routine.routineId IN :routineIds " +
       "AND dr.member.id = :memberId AND dr.performedDate = :date " +
       "AND dr.performanceLevel = 'FULL_SUCCESS'")
List<DailyRoutineEntity> findSuccessRecordsByRoutinesAndMemberAndDate(
    @Param("routineIds") List<Long> routineIds,
    @Param("memberId") Long memberId,
    @Param("date") LocalDate date);
```

### 1.4 🆕 GrowthSettings에 currentCycleDays 추가

#### 📁 **파일 수정**: `GrowthSettings.java`
```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrowthSettings {
    // 기존 필드들...
    
    @Column(name = "current_cycle_days")
    private Integer currentCycleDays = 0;  // 🆕 현재 주기 내 연속일
    
    /**
     * 목표치 증가 실행
     */
    public Integer increaseTarget() {
        this.targetValue += targetIncrement;
        this.currentCycleDays = 0; // 🔄 성장 주기 리셋
        return this.targetValue;
    }
    
    /**
     * 현재 주기 연속일 증가
     */
    public void incrementCurrentCycleDays() {
        this.currentCycleDays = (this.currentCycleDays != null ? this.currentCycleDays : 0) + 1;
    }
    
    /**
     * 현재 주기 연속일 리셋 (실패 시)
     */
    public void resetCurrentCycleDays() {
        this.currentCycleDays = 0;
    }
    
    /**
     * 성장 주기 완료 여부 확인
     */
    public boolean isGrowthCycleCompleted() {
        if (!isEnabled() || currentCycleDays == null || growthCycleDays == null) {
            return false;
        }
        return currentCycleDays > 0 && currentCycleDays % growthCycleDays == 0;
    }
}
```

### 1.5 🆕 일일 기록 저장 시 currentCycleDays 업데이트

#### 📁 **파일 수정**: `DailyRoutineService.java`
```java
/**
 * 성장 모드 루틴의 currentCycleDays 업데이트
 */
private void updateCurrentCycleDays(RoutineEntity routine, PerformanceLevel performanceLevel) {
    // 성장 모드가 아니면 업데이트하지 않음
    if (!routine.isGrowthModeEnabled()) {
        return;
    }
    
    // FULL_SUCCESS인 경우 증가, 아니면 리셋
    if (performanceLevel == PerformanceLevel.FULL_SUCCESS) {
        routine.getGrowthSettings().incrementCurrentCycleDays();
    } else {
        routine.getGrowthSettings().resetCurrentCycleDays();
    }
}
```

---

## **2단계: 핵심 비즈니스 로직 (Day 1 - 오후)**

### 2.1 성장 확인 서비스 구현

#### 📁 **신규 파일**: `RoutineGrowthCheckService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineGrowthCheckService {
    
    private final RoutineRepository routineRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    
    public GrowthCheckResponse checkGrowthReadyRoutines(Long memberId) {
        // 1. 성장 모드 활성화된 루틴들 조회
        List<RoutineEntity> growthRoutines = routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);
        
        if (growthRoutines.isEmpty()) {
            return GrowthCheckResponse.builder()
                .growthReadyRoutines(Collections.emptyList())
                .totalGrowthReadyCount(0)
                .build();
        }
        
        // 2. 전날 날짜 계산
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // 3. 배치로 전날 성공 기록 조회 (성능 최적화)
        List<Long> routineIds = growthRoutines.stream()
            .map(RoutineEntity::getRoutineId)
            .toList();
        
        List<DailyRoutineEntity> yesterdayRecords = dailyRoutineRepository
            .findSuccessRecordsByRoutinesAndMemberAndDate(routineIds, memberId, yesterday);
        
        Map<Long, DailyRoutineEntity> recordMap = yesterdayRecords.stream()
            .collect(Collectors.toMap(
                record -> record.getRoutine().getRoutineId(),
                record -> record
            ));
        
        // 4. 각 루틴별 성장 주기 완료 확인
        List<GrowthReadyRoutineResponse> growthReadyRoutines = growthRoutines.stream()
            .filter(routine -> {
                DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                return isGrowthCycleCompleted(routine, lastRecord);
            })
            .map(routine -> {
                DailyRoutineEntity lastRecord = recordMap.get(routine.getRoutineId());
                return GrowthReadyRoutineResponse.from(routine, lastRecord);
            })
            .toList();
        
        return GrowthCheckResponse.builder()
            .growthReadyRoutines(growthReadyRoutines)
            .totalGrowthReadyCount(growthReadyRoutines.size())
            .build();
    }
    
    private boolean isGrowthCycleCompleted(RoutineEntity routine, DailyRoutineEntity lastRecord) {
        // 1. 전날 성공 기록이 없으면 성장 불가
        if (lastRecord == null) {
            return false;
        }
        
        // 2. 🔄 GrowthSettings의 isGrowthCycleCompleted 메서드 사용
        // currentCycleDays 기반으로 성장 주기 완료 여부 확인
        return routine.getGrowthSettings().isGrowthCycleCompleted();
    }
    
    // 성장 주기 완료 여부만 확인하는 유틸리티 메서드 (다른 서비스에서도 사용 가능)
    public boolean isGrowthCycleCompleted(Long routineId, Long memberId, LocalDate targetDate) {
        Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
            .findSuccessRecordByRoutineAndMemberAndDate(routineId, memberId, targetDate);
            
        if (lastRecord.isEmpty()) {
            return false;
        }
        
        RoutineEntity routine = routineRepository.findById(routineId)
            .orElseThrow(() -> new BaseException(ErrorCode.ROUTINE_NOT_FOUND));
            
        return isGrowthCycleCompleted(routine, lastRecord.get());
    }
}
```

### 2.2 목표 증가 서비스 확장

#### 📁 **파일 수정**: `RoutineGrowthService.java`
```java
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {
    
    private final RoutineRepository routineRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineHelper routineHelper;
    
    // 기존 메서드들 유지...
    
    @Transactional
    public IncreaseTargetResponse increaseRoutineTarget(Long routineId, Long memberId) {
        // 1. 루틴 조회 및 기본 검증
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
        // 2. 성장 조건 검증 (모든 검증을 한 곳에서 처리)
        validateGrowthConditions(routine, memberId);
        
        // 3. 목표치 증가 실행
        Integer previousTarget = routine.getTargetValue();
        routine.increaseTarget(); // 🔄 increaseTarget() 메서드가 currentCycleDays를 0으로 리셋
        
        // 4. 로그 기록
        log.info("Target increased for routine: {} from {} to {} by member: {}", 
            routineId, previousTarget, routine.getTargetValue(), memberId);
        
        // 5. 응답 생성
        return IncreaseTargetResponse.from(routine, previousTarget);
    }
    
    @Transactional  // 🆕 성장 주기 리셋 메서드 추가
    public ResetGrowthCycleResponse resetGrowthCycle(Long routineId, Long memberId) {
        // 1. 루틴 조회 및 기본 검증
        RoutineEntity routine = routineHelper.findRoutineByIdAndMemberId(routineId, memberId);
        
        // 2. 성장 조건 검증 (목표 증가와 동일한 조건)
        validateGrowthConditions(routine, memberId);
        
        // 3. 현재 주기 리셋 실행
        Integer previousCycleDays = routine.getCurrentCycleDays();
        routine.getGrowthSettings().resetCurrentCycleDays();
        
        // 4. 로그 기록
        log.info("Growth cycle reset for routine: {} (previous cycle days: {}) by member: {}", 
            routineId, previousCycleDays, memberId);
        
        // 5. 응답 생성
        return ResetGrowthCycleResponse.from(routine, previousCycleDays);
    }
    
    private void validateGrowthConditions(RoutineEntity routine, Long memberId) {
        // 1. 성장 모드 확인
        if (!routine.isGrowthModeEnabled()) {
            throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
        }
        
        // 2. 목표 증가 가능 여부 확인
        if (!routine.canIncreaseTarget()) {
            throw new BaseException(ErrorCode.ROUTINE_CANNOT_INCREASE_TARGET);
        }
        
        // 3. 🔄 성장 주기 완료 확인 (GrowthSettings의 isGrowthCycleCompleted 메서드 사용)
        if (!routine.getGrowthSettings().isGrowthCycleCompleted()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }
        
        // 4. 전날 성공 기록 확인 (추가 안전장치)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
            .findSuccessRecordByRoutineAndMemberAndDate(routine.getRoutineId(), memberId, yesterday);
        
        if (lastRecord.isEmpty()) {
            throw new BaseException(ErrorCode.GROWTH_CYCLE_NOT_COMPLETED);
        }
    }
}
```

---

## **3단계: API 엔드포인트 구현 (Day 2 - 오전)**

### 3.1 Controller 메서드 추가

#### 📁 **파일 수정**: `RoutineController.java`
```java
@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
@Tag(name = "Routine", description = "루틴 관리 API")
public class RoutineController {
    
    private final RoutineGrowthCheckService growthCheckService;
    private final RoutineGrowthService routineGrowthService;
    private final JwtMemberExtractor jwtMemberExtractor;
    
    // 기존 메서드들 유지...
    
    @GetMapping("/growth-check")
    @Operation(summary = "성장 가능한 루틴 조회", 
               description = "로그인 시 전날 기준으로 성장 주기를 완료한 루틴들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", 
                 content = @Content(schema = @Schema(implementation = GrowthCheckResponse.class)))
    public ResponseEntity<CommonApiResponse<GrowthCheckResponse>> checkGrowthReadyRoutines(
            Authentication authentication) {
        
        Long memberId = jwtMemberExtractor.extractMemberId(authentication);
        GrowthCheckResponse response = growthCheckService.checkGrowthReadyRoutines(memberId);
        
        return ResponseEntity.ok(CommonApiResponse.success(ApiSuccessCode.SUCCESS, response));
    }
    
    @PatchMapping("/{routineId}/increase-target")
    @Operation(summary = "루틴 목표치 증가", 
               description = "성장 주기가 완료된 루틴의 목표치를 증가시킵니다.")
    @ApiResponse(responseCode = "200", description = "증가 성공",
                 content = @Content(schema = @Schema(implementation = IncreaseTargetResponse.class)))
    public IncreaseTargetResponse increaseTarget(
            HttpServletRequest request,
            @Parameter(description = "루틴 ID", example = "1") @PathVariable Long routineId) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineGrowthService.increaseRoutineTarget(routineId, userId);
    }
    
    @PatchMapping("/{routineId}/reset-growth-cycle")  // 🆕 성장 주기 리셋 API
    @Operation(summary = "성장 주기 리셋", 
               description = "성장 주기가 완료된 루틴의 주기를 리셋합니다. 성장을 거부할 때 사용합니다.")
    @ApiResponse(responseCode = "200", description = "리셋 성공",
                 content = @Content(schema = @Schema(implementation = ResetGrowthCycleResponse.class)))
    public ResetGrowthCycleResponse resetGrowthCycle(
            HttpServletRequest request,
            @Parameter(description = "루틴 ID", example = "1") @PathVariable Long routineId) {
        Long userId = jwtMemberExtractor.extractMemberId(request);
        return routineGrowthService.resetGrowthCycle(routineId, userId);
    }
}
```

### 3.2 에러 코드 추가

#### 📁 **파일 수정**: `ErrorCode.java`
```java
public enum ErrorCode {
    // 기존 에러 코드들...
    
    // 성장 루틴 관련 에러
    ROUTINE_NOT_GROWTH_MODE("ROUTINE007", "성장 모드가 활성화되지 않은 루틴입니다", HttpStatus.BAD_REQUEST.value()),
    GROWTH_CYCLE_NOT_COMPLETED("ROUTINE008", "아직 성장 주기가 완료되지 않았습니다", HttpStatus.BAD_REQUEST.value());
}
```

### 3.3 🆕 성장 주기 리셋 Response DTO 추가

#### 📁 **신규 파일**: `ResetGrowthCycleResponse.java`
```java
@Getter
@Builder
@AllArgsConstructor
public class ResetGrowthCycleResponse {
    private Long routineId;
    private String title;
    private Integer currentTarget;
    private TargetType targetType;
    private Integer growthCycleDays;
    private Integer currentCycleDays;  // 리셋 후 0
    private String message;
    
    public static ResetGrowthCycleResponse from(RoutineEntity routine, Integer previousCycleDays) {
        String targetUnit = routine.getTargetType().getUnit();
        String message = String.format("성장 주기가 리셋되었습니다. %d%s 목표로 새로운 %d일 주기를 시작하세요!", 
                routine.getTargetValue(), targetUnit, routine.getGrowthCycleDays());
                
        return ResetGrowthCycleResponse.builder()
                .routineId(routine.getRoutineId())
                .title(routine.getTitle())
                .currentTarget(routine.getTargetValue())
                .targetType(routine.getTargetType())
                .growthCycleDays(routine.getGrowthCycleDays())
                .currentCycleDays(routine.getCurrentCycleDays())  // 0
                .message(message)
                .build();
    }
}
```

---

## **4단계: 테스트 코드 작성 (Day 2 - 오후)**

### 4.1 단위 테스트

#### 📁 **신규 파일**: `RoutineGrowthCheckServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class RoutineGrowthCheckServiceTest {
    
    @Mock private RoutineRepository routineRepository;
    @Mock private DailyRoutineRepository dailyRoutineRepository;
    @InjectMocks private RoutineGrowthCheckService growthCheckService;
    
    @Test
    @DisplayName("성장 주기 완료된 루틴이 있는 경우 정상 반환")
    void checkGrowthReadyRoutines_WithCompletedGrowthCycle_ReturnsGrowthReadyRoutines() {
        // Given
        Long memberId = 1L;
        RoutineEntity growthRoutine = createGrowthRoutine();
        DailyRoutineEntity completedRecord = createCompletedRecord(7); // 7일 주기 완료
        
        when(routineRepository.findGrowthEnabledRoutinesByMemberId(memberId))
            .thenReturn(List.of(growthRoutine));
        when(dailyRoutineRepository.findSuccessRecordsByRoutinesAndMemberAndDate(any(), any(), any()))
            .thenReturn(List.of(completedRecord));
        
        // When
        GrowthCheckResponse result = growthCheckService.checkGrowthReadyRoutines(memberId);
        
        // Then
        assertThat(result.getTotalGrowthReadyCount()).isEqualTo(1);
        assertThat(result.getGrowthReadyRoutines()).hasSize(1);
        
        GrowthReadyRoutineResponse routineResponse = result.getGrowthReadyRoutines().get(0);
        assertThat(routineResponse.getConsecutiveDays()).isEqualTo(7);
        assertThat(routineResponse.getCompletedCycleDays()).isEqualTo(7);
    }
    
    @Test
    @DisplayName("성장 주기가 완료되지 않은 경우 빈 목록 반환")
    void checkGrowthReadyRoutines_WithIncompleteGrowthCycle_ReturnsEmptyList() {
        // Given
        Long memberId = 1L;
        RoutineEntity growthRoutine = createGrowthRoutine();
        DailyRoutineEntity incompleteRecord = createCompletedRecord(5); // 7일 주기 미완료
        
        when(routineRepository.findGrowthEnabledRoutinesByMemberId(memberId))
            .thenReturn(List.of(growthRoutine));
        when(dailyRoutineRepository.findSuccessRecordsByRoutinesAndMemberAndDate(any(), any(), any()))
            .thenReturn(List.of(incompleteRecord));
        
        // When
        GrowthCheckResponse result = growthCheckService.checkGrowthReadyRoutines(memberId);
        
        // Then
        assertThat(result.getTotalGrowthReadyCount()).isEqualTo(0);
        assertThat(result.getGrowthReadyRoutines()).isEmpty();
    }
    
    private RoutineEntity createGrowthRoutine() {
        return RoutineEntity.builder()
            .routineId(1L)
            .details(RoutineDetails.of("테스트 루틴", "설명", RoutineCategory.HEALTH))
            .growthSettings(GrowthSettings.of(TargetType.COUNT, 10, 7, 5))
            .build();
    }
    
    private DailyRoutineEntity createCompletedRecord(int consecutiveDays) {
        return DailyRoutineEntity.builder()
            .consecutiveDays(consecutiveDays)
            .performanceLevel(PerformanceLevel.FULL_SUCCESS)
            .performedDate(LocalDate.now().minusDays(1))
            .build();
    }
}
```

#### 📁 **신규 파일**: `RoutineGrowthServiceTest.java`
```java
@ExtendWith(MockitoExtension.class)
class RoutineGrowthServiceTest {
    
    @Mock private RoutineRepository routineRepository;
    @Mock private RoutineGrowthCheckService growthCheckService;
    @Mock private RoutineHelper routineHelper;
    @InjectMocks private RoutineGrowthService routineGrowthService;
    
    @Test
    @DisplayName("조건을 만족하는 경우 목표 증가 성공")
    void increaseRoutineTarget_WithValidConditions_Success() {
        // Given
        Long routineId = 1L;
        Long memberId = 1L;
        RoutineEntity routine = createGrowthRoutine(20, 5); // 현재 20, 증가량 5
        
        when(routineHelper.findRoutineByIdAndMemberId(routineId, memberId))
            .thenReturn(routine);
        when(growthCheckService.canIncreaseTarget(routineId, memberId))
            .thenReturn(true);
        
        // When
        IncreaseTargetResponse result = routineGrowthService.increaseRoutineTarget(routineId, memberId);
        
        // Then
        assertThat(result.getPreviousTarget()).isEqualTo(20);
        assertThat(result.getNewTarget()).isEqualTo(25);
        assertThat(result.getIncrement()).isEqualTo(5);
        assertThat(result.getMessage()).contains("20개에서 25개로 증가");
    }
    
    @Test
    @DisplayName("성장 주기가 완료되지 않은 경우 예외 발생")
    void increaseRoutineTarget_WithIncompleteGrowthCycle_ThrowsException() {
        // Given
        Long routineId = 1L;
        Long memberId = 1L;
        RoutineEntity routine = createGrowthRoutine(20, 5);
        
        when(routineHelper.findRoutineByIdAndMemberId(routineId, memberId))
            .thenReturn(routine);
        when(growthCheckService.canIncreaseTarget(routineId, memberId))
            .thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> routineGrowthService.increaseRoutineTarget(routineId, memberId))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("성장 주기가 완료되지 않았습니다");
    }
}
```

### 4.2 통합 테스트

#### 📁 **신규 파일**: `RoutineGrowthIntegrationTest.java`
```java
@SpringBootTest
@Transactional
@TestMethodOrder(OrderAnnotation.class)
class RoutineGrowthIntegrationTest {
    
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private RoutineRepository routineRepository;
    @Autowired private DailyRoutineRepository dailyRoutineRepository;
    @Autowired private MemberRepository memberRepository;
    
    private String jwtToken;
    private Long memberId;
    private Long routineId;
    
    @BeforeEach
    void setUp() {
        // JWT 토큰 생성 및 테스트 데이터 설정
    }
    
    @Test
    @Order(1)
    @DisplayName("성장 확인 API 통합 테스트")
    void testGrowthCheckAPI() {
        // Given: 7일 연속 성공 기록 생성
        createConsecutiveSuccessRecords(7);
        
        // When
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/routines/growth-check",
            HttpMethod.GET,
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // JSON 파싱 및 응답 검증
    }
    
    @Test
    @Order(2) 
    @DisplayName("목표 증가 API 통합 테스트")
    void testIncreaseTargetAPI() {
        // Given: 성장 조건을 만족하는 루틴
        createConsecutiveSuccessRecords(7);
        
        // When
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/routines/" + routineId + "/increase-target",
            HttpMethod.PATCH,
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // DB에서 목표값 증가 확인
        RoutineEntity updatedRoutine = routineRepository.findById(routineId).orElseThrow();
        assertThat(updatedRoutine.getTargetValue()).isEqualTo(25); // 20 + 5
    }
}
```

---

## **5단계: 최종 검증 및 문서화 (Day 3)**

### 5.1 성능 테스트
- [ ] 대량 루틴 데이터에서 성장 확인 API 성능 측정
- [ ] 캐싱 동작 확인
- [ ] 데이터베이스 쿼리 실행 계획 검토

### 5.2 보안 테스트
- [ ] 다른 사용자 루틴 접근 차단 확인
- [ ] JWT 토큰 없이 API 접근 차단 확인
- [ ] 권한 검증 로직 테스트

### 5.3 API 문서 업데이트
- [ ] Swagger 문서 자동 생성 확인
- [ ] Postman 컬렉션 생성
- [ ] API 명세서 최종 검토

---

## 🧪 테스트 시나리오

### 핵심 테스트 케이스
1. **성장 주기 정확히 완료** (7일 주기, currentCycleDays = 7)
2. **성장 주기 초과 완료** (7일 주기, currentCycleDays = 14)  
3. **성장 주기 미완료** (7일 주기, currentCycleDays = 5)
4. **성장 모드 비활성화** 루틴
5. **다른 사용자 루틴 접근** 시도
6. **존재하지 않는 루틴** 접근 시도
7. **🆕 성장 주기 리셋 테스트** (성장 거부 시나리오)
8. **🆕 성장 후 currentCycleDays 리셋 확인** (목표 증가 후 주기 초기화)

### 경계값 테스트 (🔄 currentCycleDays 기준)
- currentCycleDays = 0 (주기 시작)
- currentCycleDays = 성장 주기 - 1 (성장 임박)
- currentCycleDays = 성장 주기 (성장 가능)
- currentCycleDays = 성장 주기 + 1 (성장 가능)
- currentCycleDays = 성장 주기 * 2 (다중 주기 완료)

### 🆕 성장 주기 리셋 시나리오
- **시나리오 A**: 성장 조건 만족 → 사용자 수락 → 목표 증가 + currentCycleDays = 0
- **시나리오 B**: 성장 조건 만족 → 사용자 거부 → 목표 유지 + currentCycleDays = 0
- **시나리오 C**: 실패 발생 → currentCycleDays = 0 자동 리셋

---

## ⚡ 성능 최적화 방안

### 데이터베이스 최적화
```sql
-- 핵심 쿼리 (배치 조회)
SELECT r.routine_id, r.target_value, r.target_increment, r.growth_cycle_days,
       dr.consecutive_days, dr.performed_date
FROM routine_table r
LEFT JOIN daily_routine_table dr ON r.routine_id = dr.routine_id 
    AND dr.member_id = r.member_id 
    AND dr.performed_date = :yesterday
    AND dr.performance_level = 'FULL_SUCCESS'
WHERE r.member_id = :memberId 
    AND r.is_growth_mode = true;
```

### 단순한 아키텍처
- **캐싱 없음**: 해커톤 프로젝트 특성상 단순성 우선
- **성능**: 사용자 수가 적고 쿼리가 단순하여 캐싱 불필요
- **유지보수**: 복잡성 제거로 개발/테스트 속도 향상

### 모니터링 지표
- API 응답 시간 (목표: 200ms 이하)
- 데이터베이스 쿼리 실행 시간
- API 호출 성공률

---

## 🚀 배포 계획

### 배포 전 체크리스트
- [ ] 모든 테스트 통과 확인
- [ ] 코드 리뷰 완료
- [ ] API 문서 업데이트
- [ ] 데이터베이스 마이그레이션 (필요시)
- [ ] 캐시 설정 확인

### 배포 후 모니터링
- [ ] API 호출 빈도 및 성공률 모니터링
- [ ] 에러 로그 모니터링
- [ ] 사용자 피드백 수집

---

## 🔄 향후 확장 계획

### Phase 2 (추후 개발)
- **배치 작업**: 매일 새벽 자동 성장 감지 및 알림 발송
- **성장 히스토리**: 목표 증가 이력 추적 및 통계
- **개인화 알고리즘**: 사용자별 최적 성장 주기 제안

### Phase 3 (장기 계획)
- **AI 기반 목표 설정**: 과거 성과 기반 자동 목표 조정
- **소셜 기능 연동**: 성장 달성 시 친구들과 공유
- **리워드 시스템**: 성장 달성에 따른 포인트/뱃지 시스템

---

## 📞 개발 일정 및 책임

| 구분 | 담당자 | 기간 | 주요 작업 |
|------|--------|------|-----------|
| **Day 1** | Backend Dev | 2025-01-31 | DTO, Repository, Service 개발 |
| **Day 2** | Backend Dev | 2025-02-01 | Controller, 테스트 코드 작성 |  
| **Day 3** | Full Team | 2025-02-02 | 통합 테스트, 문서화, 배포 |

### 마일스톤
- **Day 1 EOD**: 핵심 비즈니스 로직 완성
- **Day 2 EOD**: API 엔드포인트 및 테스트 완성  
- **Day 3 EOD**: 배포 준비 완료

---

---

## 📝 개발 과정 중 주요 변경사항

### 🔄 아키텍처 개선 (실제 구현 반영)

#### 1. **currentCycleDays 필드 도입**
**문제**: 기존 consecutiveDays만으로는 성장 후 주기 리셋 불가능
**해결**: GrowthSettings에 currentCycleDays 필드 추가
```java
// 변경 전: consecutiveDays만 사용 (문제 있음)
if (consecutiveDays >= growthCycleDays) { /* 성장 가능하지만 리셋 불가 */ }

// 변경 후: currentCycleDays 별도 관리 (해결)
if (currentCycleDays >= growthCycleDays) { /* 성장 가능 */ }
router.increaseTarget(); // currentCycleDays = 0 으로 리셋
```

#### 2. **성장 주기 리셋 API 추가**
**요구사항**: 사용자가 성장을 거부할 때 주기를 초기화하는 기능 필요
**구현**: `/api/routines/{routineId}/reset-growth-cycle` PATCH 엔드포인트 추가
- `ResetGrowthCycleResponse` DTO 생성
- `RoutineGrowthService.resetGrowthCycle()` 메서드 추가
- 성장 조건 검증 로직 재사용

#### 3. **일일 기록 저장 로직 확장**
**변경점**: `DailyRoutineService`에서 currentCycleDays 자동 관리
```java
// FULL_SUCCESS → currentCycleDays 증가
// 실패 → currentCycleDays = 0 리셋
private void updateCurrentCycleDays(RoutineEntity routine, PerformanceLevel level) {
    if (level == PerformanceLevel.FULL_SUCCESS) {
        routine.getGrowthSettings().incrementCurrentCycleDays();
    } else {
        routine.getGrowthSettings().resetCurrentCycleDays();
    }
}
```

### 🛠️ 실제 구현 결과

#### ✅ 완료된 작업
1. **Response DTOs**: 4개 클래스 완성
   - `GrowthCheckResponse`, `GrowthReadyRoutineResponse`
   - `IncreaseTargetResponse`, `ResetGrowthCycleResponse`

2. **Entity 확장**: GrowthSettings에 currentCycleDays 필드 추가
   - `incrementCurrentCycleDays()`, `resetCurrentCycleDays()` 메서드
   - `isGrowthCycleCompleted()` 로직 개선

3. **Service Layer**: 2개 서비스 클래스 구현
   - `RoutineGrowthCheckService`: 성장 감지 전용
   - `RoutineGrowthService`: 목표 증가 + 주기 리셋

4. **API 엔드포인트**: 3개 REST API 완성
   - `GET /growth-check`: 성장 가능한 루틴 조회
   - `PATCH /{id}/increase-target`: 목표 증가
   - `PATCH /{id}/reset-growth-cycle`: 주기 리셋

5. **Repository 쿼리**: 성능 최적화된 배치 조회 메서드 추가

6. **Error Handling**: 2개 전용 에러 코드 추가
   - `ROUTINE_NOT_GROWTH_MODE`, `GROWTH_CYCLE_NOT_COMPLETED`

### 🎯 핵심 성과

#### 비즈니스 로직 정확성
- **성장 주기 완료 감지**: currentCycleDays % growthCycleDays == 0
- **자동 주기 리셋**: 목표 증가 시 currentCycleDays = 0
- **실패 시 리셋**: 성공 기록 실패 시 currentCycleDays = 0
- **사용자 선택권**: 성장 수락/거부 모두 지원

#### 성능 최적화
- **배치 쿼리**: 여러 루틴을 한 번의 쿼리로 조회
- **단순한 로직**: 복잡한 날짜 계산 대신 정수 카운터 사용
- **최소한의 DB 접근**: 필요한 데이터만 조회

#### 사용자 경험
- **명확한 피드백**: 성장 전후 목표값 비교 메시지
- **유연한 선택**: 성장 수락 또는 거부 모두 가능
- **일관된 API**: CommonApiResponse 구조 준수

---

**개발 완료일**: 2025-01-31  
**주요 기여자**: Backend Development Team  
**다음 단계**: Postman 테스트 → 프론트엔드 연동