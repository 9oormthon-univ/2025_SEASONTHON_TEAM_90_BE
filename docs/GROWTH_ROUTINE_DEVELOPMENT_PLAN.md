# Routine 도메인 리팩토링 개발 계획 (v2.1 - 하이브리드 접근법)

> **v2.1 업데이트**: Claude vs Gemini 논쟁을 거쳐 **실무적 우선순위 + 점진적 개선**의 하이브리드 접근법 채택

## 📋 현황 분석 결과

### 현재 아키텍처의 장점
- ✅ DDD 원칙을 잘 적용한 도메인 구조
- ✅ Facade 패턴을 통한 복잡한 비즈니스 로직 분리
- ✅ Value Object(`RoutineDetails`, `GrowthSettings`)를 통한 데이터 캡슐화
- ✅ 명확한 레이어 분리 (Controller → Service → Facade → Repository)

### 주요 개선이 필요한 영역

#### 1. 책임 분리 (Separation of Concerns) 이슈
- **문제**: `RoutineController`가 3개의 서비스(`RoutineService`, `RoutineGrowthCheckService`, `RoutineGrowthService`)를 직접 의존
- **문제**: 성장 관련 검증 로직이 여러 서비스에 분산
- **파일 위치**: `src/main/java/com/groomthon/habiglow/domain/routine/controller/RoutineController.java:48-53`

#### 2. 추상화 (Abstraction) 부족
- **문제**: 성장 모드 처리 로직이 `RoutineGrowthService`와 `RoutineGrowthCheckService`에 중복
- **문제**: Repository 추상화 부족으로 복잡한 쿼리 로직이 서비스에 노출

#### 3. 복잡성 (Complexity) 증가
- **문제**: `RoutineController`에 12개의 엔드포인트가 집중
- **문제**: `RoutineGrowthCheckService.checkGrowthReadyRoutines()` 메서드가 너무 많은 책임을 가짐
- **파일 위치**: `src/main/java/com/groomthon/habiglow/domain/routine/service/RoutineGrowthCheckService.java:35-82`

#### 4. 코드 중복 (Code Duplication) 패턴
- **중복 1**: 성장 조건 검증 로직 중복
  - `RoutineGrowthService.validateGrowthConditions()` (127-151줄)
  - `RoutineGrowthCheckService.isGrowthCycleCompleted()` (84-92줄)
- **중복 2**: Repository 접근 패턴 중복
- **중복 3**: 로깅 패턴 중복

---

## 🎯 리팩토링 목표

1. **단일 책임 원칙 강화**: 각 클래스가 하나의 명확한 책임만 가지도록 개선
2. **중복 코드 제거**: 공통 로직을 추출하여 재사용성 향상
3. **복잡성 감소**: 큰 메서드와 클래스를 작은 단위로 분해
4. **테스트 용이성**: 의존성 주입과 모킹이 쉬운 구조로 개선

---

## 📊 Claude vs Gemini: 설계 철학 논쟁 결과

### 🥊 논쟁의 핵심

| 관점 | Claude (실용주의) | Gemini (완벽주의) |
|------|------------------|------------------|
| **접근법** | 점진적 개선, 즉시 실행 가능 | 완벽한 아키텍처, 한 번에 해결 |
| **우선순위** | 비즈니스 가치 → 기술적 완성도 | 기술적 완성도 → 비즈니스 가치 |
| **현실성** | 해커톤 팀 프로젝트 맥락 고려 | 대기업급 프로덕션 수준 지향 |

### 🎯 하이브리드 결론
- **즉시 실행**: Claude의 v2.0 (핵심 문제 해결)
- **점진적 채택**: Gemini의 좋은 아이디어들을 단계별로 적용
- **상황 맞춤**: 프로젝트 규모와 팀 역량에 맞는 현실적 접근

---

## 🛠️ 3단계 하이브리드 리팩토링 계획

> **핵심 원칙**: 실행 가능한 개선을 우선하되, 장기적 확장성도 고려

### ⭐ Phase 1: 서비스 레이어 통합 및 책임 재정의 (핵심)

#### 1.1 성장 관련 서비스 통합
**목표**: 분산된 `RoutineGrowthCheckService`와 `RoutineGrowthService`를 하나로 통합하여 응집도 향상

**작업 내용**:
```java
// 기존: 2개의 분리된 서비스
- RoutineGrowthCheckService (성장 조건 체크)
- RoutineGrowthService (성장 실행)

// 리팩토링 후: 1개의 통합 서비스
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {
    
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineRepository routineRepository;
    private final RoutineHelper routineHelper;

    // 기존 두 서비스의 기능을 모두 통합
    public GrowthCheckResponse checkGrowthReadyRoutines(Long memberId) {
        // 기존 RoutineGrowthCheckService.checkGrowthReadyRoutines() 로직
    }
    
    public IncreaseTargetResponse increaseRoutineTarget(Long routineId, Long memberId) {
        // 기존 RoutineGrowthService.increaseRoutineTarget() 로직
        validateGrowthConditions(routine, memberId); // private 메서드로
    }
    
    public ResetGrowthCycleResponse resetGrowthCycle(Long routineId, Long memberId) {
        // 기존 RoutineGrowthService.resetGrowthCycle() 로직
        validateGrowthConditions(routine, memberId); // private 메서드로
    }
    
    // 중복된 검증 로직은 private 메서드로 통합
    private void validateGrowthConditions(RoutineEntity routine, Long memberId) {
        // 기존 검증 로직들을 하나로 통합
    }
    
    private boolean isGrowthCycleCompleted(RoutineEntity routine, DailyRoutineEntity lastRecord) {
        // 공통 로직 통합
    }
}
```

**기대 효과**:
- ✅ 성장 관련 기능의 책임이 하나의 클래스로 모여 코드 이해 및 수정이 쉬워짐
- ✅ 불필요한 Facade 레이어 없이 구조 단순화
- ✅ 중복 코드 제거

### Phase 2: Repository 역할 명확화

#### 2.1 기존 Repository 구조 유지
**목표**: Repository는 순수 데이터 접근에만 집중하고, 비즈니스 로직은 서비스에서 처리

**작업 내용**:
```java
// ❌ 잘못된 접근 (v1.0에서 제안했던 복잡한 쿼리)
// MOD() 연산과 비즈니스 로직이 섞인 쿼리는 사용하지 않음

// ✅ 올바른 접근: 기존 구현 유지
// 1. RoutineRepository.findGrowthEnabledRoutinesByMemberId()
// 2. DailyRoutineRepository의 배치 조회 쿼리
// 3. 날짜 계산('어제')은 서비스 레이어에서 처리
```

**핵심 원칙**:
- Repository는 단순한 데이터 조회만 담당
- 비즈니스 로직(`LocalDate.now().minusDays(1)` 등)은 서비스에서 처리
- **기존 구현이 더 정확하므로 그대로 유지**

### 🚀 Phase 3: 테스트 용이성 개선 (Gemini 아이디어 채택)

#### 3.1 Clock 주입을 통한 시간 제어
**목표**: `LocalDate.now()` 직접 사용을 피해 테스트 가능한 코드로 개선

**작업 내용**:
```java
@Service
@RequiredArgsConstructor
public class RoutineGrowthService {
    
    private final Clock clock; // 새로 추가
    private final DailyRoutineRepository dailyRoutineRepository;
    private final RoutineRepository routineRepository;
    private final RoutineHelper routineHelper;

    public GrowthCheckResponse checkGrowthReadyRoutines(Long memberId) {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1); // 주입된 clock 사용
        // ... 기존 로직
    }
}

// Configuration 클래스에 Bean 등록
@Configuration
public class TimeConfig {
    
    @Bean
    @Primary
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
```

**테스트 코드 예시**:
```java
@Test
void checkGrowthReadyRoutines_특정날짜기준_테스트() {
    // Given: 2025년 9월 1일로 시간 고정
    Clock fixedClock = Clock.fixed(
        LocalDate.of(2025, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    );
    
    RoutineGrowthService service = new RoutineGrowthService(fixedClock, ...);
    
    // When & Then: 예측 가능한 테스트 수행
}
```

#### 3.2 컨트롤러 의존성 단순화
**작업 내용**:
```java
// 리팩토링 후 RoutineController
@RestController
public class RoutineController {
    private final RoutineService routineService;
    private final RoutineGrowthService routineGrowthService;  // 통합된 하나의 서비스
}
```

### 🎯 Phase 4: 사용자 경험 개선 (선택적 적용)

#### 4.1 세분화된 에러 코드 (Gemini 아이디어 채택)
**목표**: 모호한 에러 메시지를 구체적으로 개선

**작업 내용**:
```java
// ErrorCode.java에 추가
GROWTH_CYCLE_INCOMPLETE("ROUTINE008", "성장 주기가 아직 완료되지 않았습니다."),
YESTERDAY_RECORD_NOT_FOUND("ROUTINE009", "어제자 성공 기록이 없습니다."),
ROUTINE_TARGET_MAX_EXCEEDED("ROUTINE010", "목표치가 최대값에 도달했습니다.");

// validateGrowthConditions 개선
private void validateGrowthConditions(RoutineEntity routine, Long memberId) {
    if (!routine.isGrowthModeEnabled()) {
        throw new BaseException(ErrorCode.ROUTINE_NOT_GROWTH_MODE);
    }
    if (!routine.getGrowthSettings().isGrowthCycleCompleted()) {
        throw new BaseException(ErrorCode.GROWTH_CYCLE_INCOMPLETE);
    }
    
    LocalDate yesterday = LocalDate.now(clock).minusDays(1);
    Optional<DailyRoutineEntity> lastRecord = dailyRoutineRepository
        .findSuccessRecordByRoutineAndMemberAndDate(
            routine.getRoutineId(), memberId, yesterday, PerformanceLevel.FULL_SUCCESS);
    
    if (lastRecord.isEmpty()) {
        throw new BaseException(ErrorCode.YESTERDAY_RECORD_NOT_FOUND);
    }
}
```

### 🛡️ Phase 5: 고급 안정성 기능 (미래 확장)

#### 5.1 Race Condition 대응 (필요시 적용)
**적용 시점**: 실제로 동시성 문제가 발생했을 때
**Gemini 제안**: `checkTime` 파라미터 추가로 API 호출 간 상태 변경 감지

**현재 판단**: 해커톤 프로젝트에서는 **Over-engineering** 
- 단일 사용자 기준으로 동시성 문제 발생 확률 극히 낮음
- API 스펙 변경으로 인한 프론트엔드 수정 부담
- **필요할 때 추가하는 것이 더 효율적**

---

## 🚫 제거된 과잉 설계 요소들

### Gemini 리뷰로 제거된 불필요한 요소들:

1. **~~RoutineGrowthFacade~~** - 단순히 메서드만 호출하는 Leaky Facade 안티패턴
2. **~~GrowthValidationService~~** - 통합된 서비스 내 private 메서드로 충분
3. **~~ResponseBuilderUtil~~** - 기존 DTO의 `from()` 메서드가 더 좋은 설계
4. **~~컨트롤러 분리~~** - 현재 규모에서 불필요한 파일 파편화
5. **~~Repository 복잡 쿼리~~** - 비즈니스 로직 누락으로 기능 오류 발생 위험
6. **~~AOP 로깅~~** - 현재 규모에 과한 설계, 디버깅 방해 가능성

---

## ⚡ 하이브리드 구현 일정 (v2.1)

| Phase | 작업 내용 | 소요 시간 | 우선순위 | 적용 시점 |
|-------|-----------|----------|-----------|-----------|
| **Phase 1** | 서비스 레이어 통합 (핵심) | 3시간 | **최고** | **즉시** |
| **Phase 2** | Repository 역할 명확화 | 1시간 | 높음 | **즉시** |
| **Phase 3** | Clock 주입 + 컨트롤러 정리 | 2시간 | 중간 | **즉시** |
| **Phase 4** | 세분화된 에러 코드 | 1.5시간 | 낮음 | **선택적** |
| **Phase 5** | Race Condition 대응 | 3시간 | 최저 | **필요시** |

### 📊 단계별 실행 전략

**🚀 즉시 실행 (Phase 1-3)**: **6시간**
- 핵심 문제 해결과 테스트 용이성 확보
- 기존 API 스펙 유지하며 내부 구조 개선

**🎯 선택적 적용 (Phase 4)**: **+1.5시간**
- 사용자 경험 개선이 필요할 때
- 에러 메시지 개선 요청이 있을 때

**🛡️ 미래 확장 (Phase 5)**: **+3시간**
- 실제 동시성 문제 발생 시에만
- 대규모 사용자 증가 후 고려

---

## ✅ 간소화된 검증 계획

### 필수 테스트 (최소 범위)
- [ ] 통합된 `RoutineGrowthService` 단위 테스트
- [ ] API 엔드포인트 기능 검증 (기존 테스트 활용)
- [ ] 성장 모드 전체 플로우 회귀 테스트

### 성능 검증
- [ ] 기존 성능 유지 확인 (성능 저하 없어야 함)
- [ ] 메모리 사용량 측정 (객체 수 감소로 개선 기대)

---

## 🎯 현실적인 성과 지표

### 주요 개선 지표
- **클래스 파일 수**: 2개 감소 (RoutineGrowthCheckService 제거)
- **의존성 복잡도**: Controller 의존성 33% 감소 (3개→2개)
- **코드 중복**: 검증 로직 중복 완전 제거
- **코드 가독성**: 성장 관련 로직이 한 곳에 모여 추적 용이

### 유지보수성
- **신규 성장 기능 추가**: 한 클래스에서 완결 가능
- **버그 수정**: 성장 관련 이슈 추적 범위 단순화

---

## 🛡️ 최소화된 리스크

### 제거된 리스크들
- ~~API 경로 변경 위험~~ → 컨트롤러 유지로 해결
- ~~쿼리 성능 저하~~ → 기존 구현 유지로 해결  
- ~~새 클래스 테스트 부담~~ → 기존 테스트 재사용으로 해결

### 남은 리스크와 대응책
**리스크**: 서비스 통합 시 기존 테스트 일부 수정 필요
**대응책**: 기존 테스트를 최대한 재활용하고, 변경 범위 최소화

---

## 📝 참고 자료

- [Domain-Driven Design 원칙](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Facade Pattern 적용 가이드](https://refactoring.guru/design-patterns/facade)
- [Spring Boot Repository 최적화](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

## 🔄 설계 철학의 진화: Claude vs Gemini

### 📈 버전별 진화 과정

| 버전 | 설계 철학 | 소요 시간 | 주요 특징 | 한계점 |
|------|-----------|----------|-----------|--------|
| **v1.0** | 과잉 설계주의 | 19시간 | 모든 패턴 적용 | 프로젝트 맥락 무시 |
| **v2.0** | 실용주의 | 4.5시간 | 최소 변경으로 최대 효과 | 테스트 용이성 부족 |
| **v2.1** | 하이브리드 접근 | **6시간 (+α)** | 실용성 + 확장성 | - |

### 🎯 v2.1의 핵심 철학

#### 1. **적응적 우선순위**
```
즉시 필요 > 중요하지만 급하지 않음 > 미래에 필요할 수도
```

#### 2. **단계적 완성도**
```
동작하는 코드 → 깨끗한 코드 → 테스트 가능한 코드 → 완벽한 아키텍처
```

#### 3. **상황 맞춤형 설계**
- **해커톤**: Phase 1-2만으로도 충분
- **스타트업**: Phase 1-3 권장  
- **대기업**: Phase 1-5 모두 적용

### 💡 두 AI의 강점 융합

| 관점 | Claude 기여 | Gemini 기여 |
|------|-------------|-------------|
| **실행성** | ✅ 즉시 적용 가능한 계획 | ✅ 장기적 확장성 고려 |
| **현실성** | ✅ 프로젝트 맥락 반영 | ✅ 프로덕션 수준 안정성 |
| **완성도** | 🔺 테스트 용이성 부족 | ✅ Clock 주입, 에러 세분화 |

### 🏆 최종 결론

> **"Right Tool for Right Job"**
> 
> 완벽한 도구는 없다. 상황에 맞는 최적의 선택이 있을 뿐이다.
> v2.1은 실무 경험과 이론적 완성도의 균형점을 찾은 결과물이다.

---

## 📚 참고자료 및 레퍼런스

### 기술 문서
- [Java Clock 클래스 활용법](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Clock.html)
- [Spring Boot Test - 시간 의존성 테스트](https://spring.io/guides/gs/testing-web/)
- [Domain-Driven Design 실무 적용](https://martinfowler.com/bliki/DomainDrivenDesign.html)

### 설계 패턴
- [점진적 리팩토링 전략](https://refactoring.com/)
- [Facade Pattern vs Service Layer](https://refactoring.guru/design-patterns/facade)

---

*작성일: 2025-08-31*  
*설계: Claude Code Assistant*  
*리뷰: Gemini AI*  
*최종 버전: v2.1 (하이브리드 접근법)*