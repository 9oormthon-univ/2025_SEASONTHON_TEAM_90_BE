# 루틴 감소 주기 추적 방법 비교 분석

## 📋 문제 상황
루틴에서 목표 감소 후 `currentCycleDays`가 리셋되어도, 감소 조건 검사가 **과거 기록 전체**를 대상으로 하기 때문에 여전히 감소 대상으로 표시되는 문제 발생

### 현재 로직의 문제점
```java
// 문제가 되는 현재 로직
private boolean isReductionCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
    // 과거 cycleDays 동안의 모든 기록을 확인 (조정 시점 무관)
    boolean hasAnySuccess = recentRecords.stream()
        .anyMatch(record -> record.getPerformanceLevel() == PerformanceLevel.FULL_SUCCESS);
    return !hasAnySuccess; // ← 조정 후에도 과거 실패 기록 때문에 true 반환
}
```

---

## 💡 해결 방안 분석

### 방안 1: 날짜 기반 추적 ⭐ **추천**

#### 구조 변경
```java
@Column(name = "last_adjusted_date")
private LocalDate lastAdjustedDate;

// 감소 시 날짜 기록
public void decreaseTarget(Integer newTargetValue) {
    this.targetValue = newTargetValue;
    this.currentCycleDays = 0;
    this.lastAdjustedDate = LocalDate.now(); // 조정 시점 기록
}
```

#### 로직 수정
```java
private boolean isReductionCycleCompleted(RoutineEntity routine, List<DailyRoutineEntity> recentRecords) {
    LocalDate lastAdjusted = routine.getGrowthSettings().getLastAdjustedDate();
    
    // 마지막 조정 이후의 기록만 확인
    Stream<DailyRoutineEntity> relevantRecords = recentRecords.stream()
        .filter(record -> lastAdjusted == null || record.getPerformedDate().isAfter(lastAdjusted));
    
    boolean hasAnySuccess = relevantRecords
        .anyMatch(record -> record.getPerformanceLevel() == PerformanceLevel.FULL_SUCCESS);
    
    return !hasAnySuccess;
}
```

#### 장점 ✅
- **정확성**: 조정 시점을 명확히 구분하여 정확한 판단
- **최소 변경**: 기존 증가 로직에 영향 없음
- **디버깅 용이**: 언제 마지막으로 조정했는지 명확히 확인 가능
- **확장성**: 향후 조정 이력 분석/통계에 활용 가능
- **단순성**: 로직이 직관적이고 이해하기 쉬움

#### 단점 ❌
- **필드 추가**: DB 컬럼 및 엔티티 필드 추가 필요
- **마이그레이션**: 기존 데이터에 대한 초기값 설정 필요

---

### 방안 2: 통합 주기 필드 (currentCycleDays 확장)

#### 구조 변경
```java
// 기존 필드 의미 확장
// 양수: 연속 성공일 (증가 방향)
// 음수: 연속 실패일 (감소 방향)
@Column(name = "current_cycle_days")
private Integer currentCycleDays = 0;
```

#### 로직 예시
```java
public void incrementCurrentCycleDays() {
    if (this.currentCycleDays >= 0) {
        this.currentCycleDays++; // 성공 시 증가
    } else {
        this.currentCycleDays = 1; // 실패 연속에서 성공으로 전환
    }
}

public void decrementCurrentCycleDays() {
    if (this.currentCycleDays <= 0) {
        this.currentCycleDays--; // 실패 시 감소
    } else {
        this.currentCycleDays = -1; // 성공 연속에서 실패로 전환
    }
}
```

#### 장점 ✅
- **필드 절약**: 추가 필드 없이 기존 필드 활용
- **통합 관리**: 하나의 필드로 증가/감소 상태 모두 표현
- **직관적**: 양수/음수로 방향성 명확히 표현

#### 단점 ❌
- **복잡성**: 기존 증가 로직 전체 수정 필요
- **혼란 가능성**: 음수 값의 의미가 혼란을 줄 수 있음
- **리팩토링 범위**: 전체 Growth 관련 로직 수정 필요

---

### 방안 3: 별도 감소 주기 필드

#### 구조 변경
```java
@Column(name = "current_cycle_days") // 기존: 증가 전용
private Integer currentCycleDays = 0;

@Column(name = "current_reduction_cycle_days") // 신규: 감소 전용
private Integer currentReductionCycleDays = 0;
```

#### 로직 분리
```java
// 증가 로직 (기존 유지)
public void incrementCurrentCycleDays() { ... }

// 감소 로직 (신규)
public void incrementReductionCycleDays() {
    this.currentReductionCycleDays++;
}

public void resetReductionCycleDays() {
    this.currentReductionCycleDays = 0;
}
```

#### 장점 ✅
- **완전 분리**: 증가/감소 로직 완전히 독립
- **기존 로직 보호**: 증가 관련 기존 로직에 영향 없음
- **명확한 책임**: 각 필드의 역할이 명확

#### 단점 ❌
- **중복성**: 유사한 역할의 필드 2개 관리
- **복잡성 증가**: 두 주기를 동시에 관리해야 함
- **데이터 일관성**: 두 필드 간 상태 동기화 복잡

---

### 방안 4: 상태 기반 접근 (State Machine)

#### 구조 변경
```java
@Enumerated(EnumType.STRING)
@Column(name = "growth_phase")
private GrowthPhase currentPhase = GrowthPhase.STABLE;

@Column(name = "phase_start_date")
private LocalDate phaseStartDate;

@Column(name = "phase_progress_days")
private Integer phaseProgressDays = 0;

public enum GrowthPhase {
    GROWING,    // 성장 중 (목표치 증가 준비)
    STABLE,     // 안정 상태
    REDUCING    // 감소 중 (목표치 감소 준비)
}
```

#### 상태 전환 로직
```java
public void transitionToGrowingPhase() {
    this.currentPhase = GrowthPhase.GROWING;
    this.phaseStartDate = LocalDate.now();
    this.phaseProgressDays = 0;
}

public void transitionToReducingPhase() {
    this.currentPhase = GrowthPhase.REDUCING;
    this.phaseStartDate = LocalDate.now();
    this.phaseProgressDays = 0;
}
```

#### 장점 ✅
- **명확한 상태**: 현재 루틴이 어떤 단계인지 명확
- **확장성**: 새로운 단계 추가 용이 (예: MAINTENANCE, BREAK)
- **비즈니스 로직 일치**: 실제 사용자 경험과 부합
- **풍부한 정보**: 단계별 세부 정보 추적 가능

#### 단점 ❌
- **큰 변경**: 기존 구조 대폭 수정 필요
- **복잡성**: State Machine 로직 구현 복잡
- **러닝 커브**: 팀 전체가 새로운 개념 학습 필요

---

## 🎯 방안별 비교표

| 구분 | 방안1(날짜) | 방안2(통합) | 방안3(분리) | 방안4(상태) |
|------|-------------|-------------|-------------|-------------|
| **구현 난이도** | ⭐⭐ 쉬움 | ⭐⭐⭐⭐ 어려움 | ⭐⭐⭐ 보통 | ⭐⭐⭐⭐⭐ 매우 어려움 |
| **기존 코드 영향** | ⭐ 최소 | ⭐⭐⭐⭐⭐ 대폭 수정 | ⭐⭐ 적음 | ⭐⭐⭐⭐⭐ 대폭 수정 |
| **정확성** | ⭐⭐⭐⭐⭐ 매우 높음 | ⭐⭐⭐⭐ 높음 | ⭐⭐⭐⭐ 높음 | ⭐⭐⭐⭐⭐ 매우 높음 |
| **유지보수성** | ⭐⭐⭐⭐ 좋음 | ⭐⭐ 나쁨 | ⭐⭐⭐ 보통 | ⭐⭐⭐ 보통 |
| **확장성** | ⭐⭐⭐ 보통 | ⭐⭐ 제한적 | ⭐⭐⭐ 보통 | ⭐⭐⭐⭐⭐ 매우 좋음 |
| **개발 시간** | 1-2시간 | 1-2일 | 4-6시간 | 2-3일 |

---

## 📋 결론 및 권장사항

### 🥇 **1순위: 방안 1 (날짜 기반 추적)**
**현재 상황에서 최적의 선택**
- 빠른 문제 해결
- 최소한의 리스크
- 높은 정확성 보장
- 향후 확장 가능성 유지

### 🥈 **2순위: 방안 3 (별도 감소 주기 필드)**
**안정성을 중시하는 경우**
- 기존 로직 완전 보호
- 명확한 책임 분리
- 중간 수준의 복잡성

### 🥉 **3순위: 방안 4 (상태 기반)**
**장기적 확장성을 고려하는 경우**
- 미래 기능 확장에 유리
- 비즈니스 로직과 높은 일치도
- 상당한 개발 투자 필요

### ❌ **비추천: 방안 2 (통합 필드)**
- 기존 로직에 큰 영향
- 혼란 가능성 높음
- 디버깅 어려움

---

## 🚀 구현 로드맵 (방안 1 기준)

### Phase 1: 구조 변경 (30분)
1. `GrowthSettings`에 `lastAdjustedDate` 필드 추가
2. `decreaseTarget()` 메서드에 날짜 기록 로직 추가

### Phase 2: 로직 수정 (1시간)
1. `isReductionCycleCompleted()` 메서드 수정
2. 테스트 케이스 작성

### Phase 3: 검증 (30분)
1. 기존 증가 로직 영향 확인
2. 통합 테스트 실행

**총 예상 소요시간: 2시간**