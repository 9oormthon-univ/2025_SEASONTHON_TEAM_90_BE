# Routine 도메인 리팩토링 개발 계획서

## 📋 프로젝트 개요

**목표**: Routine 도메인의 코드 품질을 안전하고 점진적으로 개선
**접근법**: "Big Bang" 대신 "Step by Step" - 기존 기능 안정성 유지하면서 구조적 개선
**기간**: 약 2-3일 (단계별 검증 포함)

---

## 🎯 1단계: 기반 정리 (즉시 실행 - 위험도 낮음)

### 📌 1-1. RoutineHelper 제거

**작업 내용**:
- [ ] `RoutineManagementFacade`에서 `RoutineHelper` 의존성 제거
- [ ] `RoutineQueryFacade`에서 `RoutineHelper` 의존성 제거  
- [ ] `RoutineGrowthService`에서 `RoutineHelper` 의존성 제거
- [ ] Repository 직접 호출로 변경
- [ ] `RoutineHelper.java` 파일 삭제
- [ ] 컴파일 오류 해결 및 테스트

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/
├── facade/RoutineManagementFacade.java
├── facade/RoutineQueryFacade.java  
├── service/RoutineGrowthService.java
└── helper/RoutineHelper.java (삭제)
```

**검증 방법**:
- [ ] 기존 API 테스트 모두 통과
- [ ] 컴파일 오류 없음
- [ ] 로그 확인하여 기능 정상 동작 확인

### 📌 1-2. 패키지 구조 정리

**작업 내용**:
- [ ] `domain/routine/common/` 패키지 생성
- [ ] `RoutineCategory.java`, `TargetType.java` → `common/` 이동
- [ ] `domain/routine/dto/response/adaptation/` 패키지 생성  
- [ ] 적응 관련 Response DTO들 이동:
  - `AdaptiveRoutineCheckResponse.java`
  - `GrowthReadyRoutineResponse.java`
  - `ReductionReadyRoutineResponse.java`
  - `RoutineAdaptationResultResponse.java`
  - `RoutineAdaptationCheckResponse.java`
- [ ] import 문 수정

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/
├── common/ (신규)
│   ├── RoutineCategory.java
│   └── TargetType.java
└── dto/response/adaptation/ (신규)
    ├── AdaptiveRoutineCheckResponse.java
    ├── GrowthReadyRoutineResponse.java
    └── ...
```

**검증 방법**:
- [ ] 컴파일 오류 없음
- [ ] IDE에서 참조 관계 정상 확인

### 📌 1-3. GrowthConfiguration 가독성 개선

**작업 내용**:
- [ ] `@Builder(toBuilder = true)` 어노테이션 추가
- [ ] 기존 update 메서드들을 `with...` 패턴으로 변경:
  ```java
  // Before
  public GrowthConfiguration updateTargetValue(Integer newTargetValue) {
      return GrowthConfiguration.builder()
          .isGrowthMode(this.isGrowthMode)
          // ... 모든 필드 반복
          .build();
  }
  
  // After  
  public GrowthConfiguration withUpdatedTarget(Integer newTargetValue) {
      return this.toBuilder()
          .targetValue(newTargetValue)
          .currentCycleDays(0)
          .lastAdjustedDate(LocalDate.now())
          .build();
  }
  ```
- [ ] 호출하는 곳들 메서드명 변경

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/
├── entity/GrowthConfiguration.java
└── service/GrowthConfigurationService.java
```

**검증 방법**:
- [ ] 기존 테스트 모두 통과
- [ ] 코드 리뷰로 가독성 개선 확인

---

## 🔧 2단계: 점진적 서비스 개선 (중간 위험도)

### 📌 2-1. 공통 데이터 접근 서비스 생성

**작업 내용**:
- [ ] `RoutineDataAccessService` 생성
- [ ] 중복되는 데이터 조회 로직 추출:
  ```java
  @Service
  public class RoutineDataAccessService {
      
      public List<RoutineEntity> findGrowthEnabledRoutines(Long memberId) {
          return routineRepository.findGrowthEnabledRoutinesByMemberId(memberId);
      }
      
      public List<DailyRoutineEntity> getRecentRecords(Long routineId, Long memberId, 
                                                       LocalDate startDate, LocalDate endDate) {
          return dailyRoutineRepository.findByRoutineAndMemberAndDateRange(
              routineId, memberId, startDate, endDate);
      }
      
      public Map<Long, DailyRoutineEntity> getSuccessRecords(List<Long> routineIds, 
                                                            Long memberId, LocalDate date) {
          // 성공 기록 조회 및 Map 변환 로직
      }
  }
  ```

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/
└── service/RoutineDataAccessService.java (신규)
```

### 📌 2-2. 기존 Analysis 서비스들 개선

**작업 내용**:
- [ ] `GrowthAnalysisService`에 `RoutineDataAccessService` 의존성 추가
- [ ] 중복 코드를 공통 서비스 호출로 변경
- [ ] `ReductionAnalysisService`에 동일한 작업 수행
- [ ] 기존 public 메서드 시그니처는 유지

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/service/
├── GrowthAnalysisService.java
└── ReductionAnalysisService.java
```

**검증 방법**:
- [ ] 기존 테스트 모두 통과
- [ ] 성능 영향 없음 확인
- [ ] 로그로 동작 정상 확인

### 📌 2-3. RoutineGrowthService 단순화

**작업 내용**:
- [ ] 두 분석 서비스의 결과를 조합하는 역할만 유지
- [ ] 불필요한 로직 제거
- [ ] 코드 간소화

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/service/
└── RoutineGrowthService.java
```

---

## 🏗️ 3단계: Facade 내부 구조 개선 (낮은 위험도)

### 📌 3-1. 메서드 그룹핑 및 주석 추가

**작업 내용**:
- [ ] `RoutineManagementFacade`에서 메서드들을 논리적으로 그룹핑
- [ ] 명확한 주석으로 영역 구분:
  ```java
  @Component
  public class RoutineManagementFacade {
      
      // ==================== LIFECYCLE MANAGEMENT ====================
      
      @Transactional
      public RoutineResponse createRoutineWithFullValidation(...) { ... }
      
      @Transactional  
      public RoutineResponse updateRoutineWithValidation(...) { ... }
      
      // ==================== OPTIMIZATION MANAGEMENT ====================
      
      @Transactional
      public RoutineAdaptationResultResponse executeRoutineAdaptation(...) { ... }
  }
  ```

### 📌 3-2. 공통 로직 private 메서드 추출

**작업 내용**:
- [ ] 각 영역별 공통 처리 로직을 private 메서드로 분리:
  ```java
  private <T> T executeLifecycleOperation(Supplier<T> operation) {
      // 생명주기 관련 공통 처리 (로깅, 검증 등)
  }
  
  private <T> T executeOptimizationOperation(Supplier<T> operation) {
      // 최적화 관련 공통 처리 (로깅, 검증 등)  
  }
  ```
- [ ] 세부 로직들을 의미있는 이름의 private 메서드로 분리

**변경 파일**:
```
src/main/java/com/groomthon/habiglow/domain/routine/facade/
└── RoutineManagementFacade.java
```

**검증 방법**:
- [ ] 기존 API 동작 100% 동일
- [ ] 코드 리뷰로 가독성 개선 확인

---

## 🧪 테스트 전략

### 단위 테스트
- [ ] 각 단계별로 기존 테스트가 모두 통과하는지 확인
- [ ] 새로 생성된 `RoutineDataAccessService` 테스트 작성
- [ ] 리팩토링 후 동작 일치 확인

### 통합 테스트  
- [ ] API 레벨에서 기존 기능 모두 정상 동작 확인
- [ ] 성능 저하 없음 확인
- [ ] 로그 확인으로 내부 동작 검증

### 회귀 테스트
- [ ] 전체 테스트 슈트 실행
- [ ] 다른 도메인에 영향 없음 확인

---

## 📊 진행 상황 추적

### 1단계 진행률: 0/3 완료
- [ ] RoutineHelper 제거
- [ ] 패키지 구조 정리  
- [ ] GrowthConfiguration 개선

### 2단계 진행률: 0/3 완료
- [ ] RoutineDataAccessService 생성
- [ ] Analysis 서비스 개선
- [ ] RoutineGrowthService 단순화

### 3단계 진행률: 0/2 완료
- [ ] 메서드 그룹핑
- [ ] private 메서드 추출

---

## 🚨 위험 관리

### 높은 주의가 필요한 부분
1. **Repository 호출 변경** (1단계): 쿼리 동작 변경 위험
2. **데이터 접근 로직 통합** (2단계): 성능 영향 가능성

### 롤백 계획
- 각 단계별로 별도 브랜치에서 작업
- 문제 발생 시 이전 단계로 즉시 복원 가능
- 기존 코드 백업 유지

### 검증 체크포인트
- [ ] 1단계 완료 후: 전체 API 테스트
- [ ] 2단계 완료 후: 성능 테스트  
- [ ] 3단계 완료 후: 최종 검증

---

## 🎯 성공 지표

### 정량적 지표
- [ ] 코드 중복 라인 수 50% 이상 감소
- [ ] API 응답 시간 성능 저하 없음 (±5% 이내)
- [ ] 테스트 커버리지 현재 수준 유지

### 정성적 지표  
- [ ] 코드 가독성 향상 (리뷰어 평가)
- [ ] 새로운 기능 추가 시 변경 범위 최소화
- [ ] 개발자 인지 부하 감소

---

## 📅 타임라인

**Day 1**: 1단계 완료 (기반 정리)
- 오전: RoutineHelper 제거
- 오후: 패키지 정리 + GrowthConfiguration 개선

**Day 2**: 2단계 완료 (서비스 개선)  
- 오전: RoutineDataAccessService 생성
- 오후: Analysis 서비스들 개선

**Day 3**: 3단계 완료 (Facade 정리)
- 오전: 메서드 그룹핑 및 구조 정리
- 오후: 최종 검증 및 문서화

**총 소요 예상 시간**: 2-3일

---

## 📝 완료 후 차세대 계획

이번 리팩토링이 완료되면:
- [ ] 성과 측정 및 회고
- [ ] 다음 개선 영역 식별 (Daily 도메인 등)
- [ ] 팀 내 리팩토링 경험 공유
- [ ] 지속적 개선 프로세스 정립

**핵심 원칙**: "완벽한 한 번"보다 "꾸준한 개선"을 추구합니다.