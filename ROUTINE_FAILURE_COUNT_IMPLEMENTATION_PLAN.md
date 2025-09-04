# Routine Failure Count 기반 감소 로직 구현 계획서

## 📋 개요

현재 루틴 감소 로직은 복잡한 날짜 기반 필터링과 `lastAdjustedDate` 계산으로 인해 리셋 후에도 감소 대상으로 계속 표시되는 문제가 있습니다. 이를 해결하기 위해 성장 주기(`currentCycleDays`)와 동일한 방식으로 실패 카운트(`failureCycleDays`)를 도입하여 감소 로직을 단순화합니다.

## 🎯 목표

1. **실패 카운트 기반 감소 체크**: 복잡한 날짜 필터링 대신 단순한 카운터 사용
2. **리셋 로직 명확화**: 실패 카운트를 0으로 설정하여 즉시 감소 대상에서 제외
3. **성장/감소 로직 일관성**: 동일한 패턴으로 성공/실패 카운트 관리
4. **유지보수성 향상**: 복잡한 날짜 기반 로직 제거로 코드 가독성 개선

## 📊 현재 아키텍처 분석

### 🔍 현재 Growth Cycle 업데이트 메커니즘
현재 `currentCycleDays`는 **DailyRoutineService**에서 직접 관리됩니다:

```java
// DailyRoutineService.java:61-73
private void updateCurrentCycleDays(RoutineEntity routine, PerformanceLevel performanceLevel) {
    if (!routine.isGrowthModeEnabled()) {
        return;
    }
    
    // FULL_SUCCESS인 경우 증가, 아니면 리셋
    if (performanceLevel == PerformanceLevel.FULL_SUCCESS) {
        routine.updateGrowthConfiguration(routine.getGrowthConfiguration().withIncrementedCycle());
    } else {
        routine.updateGrowthConfiguration(routine.getGrowthConfiguration().withResetCycle());
    }
}
```

**호출 흐름**: API → DailyRecordFacade → DailyRecordCommandService → DailyRecordDomainService → DailyRoutineService

### 기존 ReductionStrategy 로직 문제
```java
// 복잡한 날짜 필터링으로 인한 문제
List<DailyRoutineEntity> relevantRecords = recentRecords.stream()
    .filter(record -> lastAdjustedDate == null || record.getPerformedDate().isAfter(lastAdjustedDate))
    .toList();

// 리셋 후 relevantRecords가 빈 배열이 되어 false 반환
if (relevantRecords.isEmpty()) {
    return false; // 문제: 리셋했는데도 계속 감소 대상으로 표시됨
}
```

### ✅ 현재 성장 로직의 장점
```java
// 단순하고 명확한 카운터 기반
public boolean isCycleCompleted() {
    return currentCycleDays >= growthCycleDays;
}
```

### 🎯 아키텍처 선택 근거
기존 패턴을 유지하는 이유:
1. **일관성**: 이미 동일한 방식으로 `currentCycleDays` 관리 중
2. **최소 변경**: Daily 도메인의 기존 로직에 실패 카운트만 추가
3. **트랜잭션 안정성**: Daily 기록과 Routine 카운터가 같은 트랜잭션에서 관리
4. **즉시 문제 해결**: 복잡한 구조 변경 없이 리셋 이슈 해결 가능

## 🏗️ 구현 계획

### 1단계: 엔티티 변경

#### 1.1 GrowthConfiguration.java 수정
**추가할 필드:**
```java
@Column(name = "failure_cycle_days")
private Integer failureCycleDays = 0;
```

**추가할 메서드:**
```java
// 실패 카운트 증가
public GrowthConfiguration withIncrementedFailureCycle() {
    return this.toBuilder()
        .failureCycleDays((this.failureCycleDays != null ? this.failureCycleDays : 0) + 1)
        .build();
}

// 실패 카운트 리셋
public GrowthConfiguration withResetFailureCycle() {
    return this.toBuilder()
        .failureCycleDays(0)
        .build();
}

// 성공 카운트 리셋
public GrowthConfiguration withResetSuccessCycle() {
    return this.toBuilder()
        .currentCycleDays(0)
        .build();
}

// 실패 주기 완료 체크
public boolean isFailureCycleCompleted() {
    if (!isEnabled() || failureCycleDays == null || growthCycleDays == null) {
        return false;
    }
    return failureCycleDays >= growthCycleDays;
}

// Builder 수정
@Builder(toBuilder = true)
private GrowthConfiguration(Boolean isGrowthMode, TargetType targetType, Integer targetValue,
    Integer growthCycleDays, Integer targetIncrement, Integer currentCycleDays,
    Integer targetDecrement, Integer minimumTargetValue, LocalDate lastAdjustedDate,
    Integer failureCycleDays) { // 새 필드 추가
    // ... 기존 로직
    this.failureCycleDays = failureCycleDays != null ? failureCycleDays : 0;
}
```

### 2단계: Daily 도메인 수정 (기존 패턴 확장)

#### 2.1 DailyRoutineService.java 메서드 수정
기존 `updateCurrentCycleDays` 메서드를 확장하여 실패 카운트도 함께 관리:

```java
// 기존 메서드명을 더 명확하게 변경
private void updateCycleDays(RoutineEntity routine, PerformanceLevel performanceLevel) {
    if (!routine.isGrowthModeEnabled()) {
        return;
    }
    
    GrowthConfiguration newConfig;
    
    if (performanceLevel == PerformanceLevel.FULL_SUCCESS) {
        // 성공 시: currentCycleDays++, failureCycleDays=0 (리셋)
        newConfig = routine.getGrowthConfiguration()
            .withIncrementedCycle()
            .withResetFailureCycle();
    } else {
        // 실패 시: failureCycleDays++, currentCycleDays=0 (리셋)
        newConfig = routine.getGrowthConfiguration()
            .withIncrementedFailureCycle() 
            .withResetSuccessCycle();
    }
    
    routine.updateGrowthConfiguration(newConfig);
    log.debug("Updated routine cycles - ID: {}, Performance: {}, Success: {}, Failure: {}", 
        routine.getRoutineId(), performanceLevel, 
        newConfig.getCurrentCycleDays(), newConfig.getFailureCycleDays());
}
```

#### 2.2 호출부 수정
```java
// saveRoutineRecords 메서드에서 호출 (기존 38라인)
updateCycleDays(record.getRoutine(), record.getPerformanceLevel()); // 메서드명만 변경
```

**장점**: 
- 기존 아키텍처 그대로 유지 
- DailyRoutineService에 로직 집중
- 트랜잭션 경계 변화 없음

### 3단계: ReductionStrategy 단순화

#### 3.1 ReductionStrategy.java 대폭 수정
```java
@Override
public boolean isAdaptationCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
    if (!routine.isGrowthModeEnabled()) {
        log.info("Routine {} is not in growth mode", routine.getRoutineId());
        return false;
    }
    
    // 단순화된 로직: 실패 카운트만 체크
    boolean isFailureCycleCompleted = routine.getGrowthConfiguration().isFailureCycleCompleted();
    
    log.info("Reduction check for routine {}: failureCycleDays={}, growthCycleDays={}, completed={}", 
        routine.getRoutineId(), 
        routine.getGrowthConfiguration().getFailureCycleDays(),
        routine.getGrowthCycleDays(),
        isFailureCycleCompleted);
    
    return isFailureCycleCompleted;
}
```

### 4단계: Facade 리셋 로직 수정

#### 4.1 RoutineManagementFacade.java 수정
```java
private RoutineAdaptationResultResponse executeGrowthReset(RoutineEntity routine, Long memberId) {
    Integer previousFailureDays = routine.getGrowthConfiguration().getFailureCycleDays();
    
    // 실패 카운트만 리셋 (성공 카운트는 유지)
    GrowthConfiguration newConfig = routine.getGrowthConfiguration()
        .withResetFailureCycle();
    routine.updateGrowthConfiguration(newConfig);
    
    log.info("Failure cycle reset for routine: {} by member: {}, previous failure days: {}",
        routine.getRoutineId(), memberId, previousFailureDays);
        
    return RoutineAdaptationResultResponse.success(
        routine.getRoutineId(), routine.getTitle(), 
        previousFailureDays, 0, AdaptationAction.RESET);
}
```

### 5단계: 데이터베이스 마이그레이션

#### 5.1 migration.sql
```sql
-- routine_table에 failure_cycle_days 컬럼 추가
ALTER TABLE routine_table 
ADD COLUMN failure_cycle_days INTEGER DEFAULT 0;

-- 기존 데이터 초기화 (모든 루틴의 실패 카운트를 0으로 설정)
UPDATE routine_table 
SET failure_cycle_days = 0 
WHERE failure_cycle_days IS NULL;

-- NOT NULL 제약조건 추가
ALTER TABLE routine_table 
ALTER COLUMN failure_cycle_days SET NOT NULL;
```

### 6단계: 기존 복잡한 로직 제거

#### 6.1 제거할 메서드들

**ReductionStrategy.java:**
- ❌ 날짜 기반 `relevantRecords` 필터링 (47-55라인)
- ❌ `daysSinceAdjustment` 계산 로직
- ❌ `lastAdjustedDate` 기반 복잡한 조건문들
- ❌ `recentRecords` 파라미터 의존성 (더 이상 필요 없음)

**ReductionAnalysisService.java:**
- ❌ `findLastAttemptDate()` 메서드 (92-97라인)
- ❌ 복잡한 기간별 기록 조회 로직

**RoutineDataAccessService.java:**
- ❌ `getGrowthCyclePeriodRecords()` - 감소 분석용 (96-101라인)
- ❌ `getRecentRecordsForLastAttempt()` (106-110라인)  
- ❌ `findMostRecentRecordDate()` (115-120라인)

#### 6.2 단순화할 인터페이스

**AdaptationStrategy.java:**
```java
// recentRecords 파라미터 제거 가능
boolean isAdaptationCycleCompleted(RoutineEntity routine); // 단순화된 시그니처
```

#### 6.2 정리 후 ReductionAnalysisService.java
```java
@Transactional(readOnly = true)
public RoutineAdaptationCheckResponse<ReductionReadyRoutineResponse> analyzeReductionReadyRoutines(Long memberId) {
    List<RoutineEntity> growthRoutines = routineDataAccessService.findGrowthEnabledRoutines(memberId);

    if (growthRoutines.isEmpty()) {
        return RoutineAdaptationCheckResponse.reduction(Collections.emptyList());
    }

    List<ReductionReadyRoutineResponse> reductionReadyRoutines = growthRoutines.stream()
        .filter(routine -> reductionStrategy.canAdapt(routine) && 
                          reductionStrategy.isAdaptationCycleCompleted(routine, Collections.emptyList()))
        .map(routine -> {
            Integer suggestedTarget = reductionStrategy.calculateNewTargetValue(routine);
            return ReductionReadyRoutineResponse.from(routine, suggestedTarget, null);
        })
        .toList();

    log.info("Reduction analysis completed for member: {}, found {} reduction-ready routines",
        memberId, reductionReadyRoutines.size());

    return RoutineAdaptationCheckResponse.reduction(reductionReadyRoutines);
}
```

## 🧪 테스트 시나리오

### 시나리오 1: 정상 감소 플로우
1. 루틴 생성 (성장 모드, 주기 7일)
2. 7일 연속 실패 → `failureCycleDays = 7` → 감소 대상 표시
3. 감소 실행 → 목표 감소, `failureCycleDays = 0`
4. 다음 조회 → 감소 대상에서 제외

### 시나리오 2: 리셋 플로우  
1. 감소 대상 루틴 확인
2. RESET 액션 실행 → `failureCycleDays = 0`
3. 즉시 다음 조회 → 감소 대상에서 제외

### 시나리오 3: 성공/실패 혼합
1. 5일 실패 → `failureCycleDays = 5`
2. 1일 성공 → `failureCycleDays = 0, currentCycleDays = 1`
3. 다시 실패 시작 → `currentCycleDays = 0, failureCycleDays++`

## 📅 구현 순서 (수정됨)

1. **1단계**: GrowthConfiguration 필드 및 메서드 추가
2. **2단계**: DailyRoutineService의 기존 메서드 확장 (실패 카운트 로직 추가)
3. **3단계**: ReductionStrategy 단순화 (카운터 기반 체크로 변경)
4. **4단계**: Facade 리셋 로직 수정 
5. **5단계**: 기존 복잡한 로직 제거 및 정리
6. **6단계**: 통합 테스트 및 검증

**예상 소요 시간**: 1-2일 (기존 아키텍처 유지로 빠른 구현 가능)

## 🔍 검증 방법

1. **단위 테스트**: 각 컴포넌트별 테스트 케이스 작성
2. **통합 테스트**: 전체 플로우 시나리오 테스트
3. **로그 분석**: 각 단계별 상세 로그로 동작 확인
4. **API 테스트**: 실제 REST API 호출로 검증

## 📋 체크리스트

### 구현 완료 체크
- [ ] GrowthConfiguration 필드 추가
- [ ] Builder 패턴 메서드 추가
- [ ] 데이터베이스 마이그레이션
- [ ] RoutineCycleUpdateService 구현
- [ ] Daily 도메인 연동
- [ ] ReductionStrategy 단순화
- [ ] Facade 리셋 로직 수정
- [ ] 기존 복잡한 로직 제거
- [ ] 테스트 케이스 작성
- [ ] 통합 테스트 검증

### 정리 완료 체크  
- [ ] 사용하지 않는 메서드 삭제
- [ ] 불필요한 import 제거
- [ ] 로그 메시지 정리
- [ ] 주석 업데이트
- [ ] 코드 리뷰 완료

이 계획대로 진행하면 루틴 감소 로직이 훨씬 단순하고 안정적으로 작동할 것입니다.