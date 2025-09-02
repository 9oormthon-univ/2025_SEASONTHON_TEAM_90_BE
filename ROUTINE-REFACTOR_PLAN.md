# 🔧 Routine 도메인 리팩토링 계획서 (개정판)

> **프로젝트**: HabiGlow Backend  
> **도메인**: Routine (루틴 관리)  
> **작성일**: 2025-01-02  
> **개정일**: 2025-01-02 (Gemini 리뷰 반영)  
> **목표**: 현실적이고 점진적인 코드 품질 향상

## 📝 **개정 배경**

**Gemini AI의 상세 리뷰**를 통해 다음과 같은 문제점을 확인했습니다:
- **과도한 엔지니어링**: Command 패턴 등 불필요한 복잡성 증가
- **기존 컨벤션 무시**: `domain/routine` 구조를 벗어난 패키지 설계
- **Big Bang Refactoring 위험**: 10일간 전면 개편의 리스크
- **비즈니스 가치 대비 과잉 투자**: 현재 요구사항을 넘어선 설계

**→  80%의 효과를 50%의 노력으로 달성하는 현실적 계획으로 수정합니다.**

## 📋 **현재 상태 종합 진단**

### **1. 심각한 문제점 분석**

#### 🚨 **RoutineGrowthService 비대화 (404줄)**
- **문제**: 성장/감소/적응형 조정 로직이 모두 하나의 서비스에 집중
- **위험도**: ⭐⭐⭐⭐⭐ (매우 높음)
- **상세 분석**:
    - `checkGrowthReadyRoutines()` - 성장 대상 조회
    - `checkAdaptiveRoutines()` - 적응형 조정 (성장+감소 통합)
    - `checkReductionReadyRoutines()` - 감소 대상 조회
    - 3개의 유사한 체크 메서드가 중복 로직 포함
    - 검증 메서드들이 여러 곳에 중복 (`validateGrowthConditions`, `validateReductionConditions`)

#### 🚨 **DTO 과다 문제 (21개 파일)**
- **문제**: 유사한 구조의 Response DTO들이 과도하게 분화
- **위험도**: ⭐⭐⭐⭐ (높음)
- **상세 분석**:
  ```
  GrowthCheckResponse          → GrowthReadyRoutineResponse 래핑
  AdaptiveRoutineCheckResponse → Growth + Reduction 래핑  
  DifficultyReductionCheckResponse → ReductionReadyRoutineResponse 래핑
  IncreaseTargetResponse       → 단순 결과 래핑
  DecreaseTargetResponse       → 단순 결과 래핑
  ResetGrowthCycleResponse     → 단순 결과 래핑
  ```
- **중복 패턴**: `totalCount` + `list` 구조 반복

#### 🚨 **컨트롤러 분산 문제**
- **문제**: 3개 컨트롤러로 API가 논리적 일관성 없이 분산
- **위험도**: ⭐⭐⭐ (중간)
- **상세 분석**:
  ```
  /api/routines           → RoutineController (기본 CRUD)
  /api/routines/growth    → RoutineGrowthController (성장 관련)
  /api/routine-categories → RoutineCategoryController (카테고리)
  ```

#### ⚠️ **로직 중복 & 책임 분산**
- **GrowthSettings**: 196줄로 비대화, 도메인 로직과 데이터 모델 혼재
- **Validation**: 검증 로직이 Service, Entity, Facade에 분산
- **Query Logic**: Repository 조회 패턴이 중복

#### ⚠️ **구조적 문제**
- **Helper 패키지**: 단순한 Repository 래퍼만 담고 있어 역할 모호
- **Facade 패턴**: 진정한 복잡성 조정보다는 단순 위임만 수행

---

## 🎯 **현실적 리팩토링 계획 (3단계)**

> **핵심 원칙**: 기존 아키텍처 존중, 점진적 개선, 실용적 접근

### **Phase 1: 가장 시급한 문제 해결** ⏳ 3일 소요

#### **1.1 RoutineGrowthService 분해 (404줄 → 150줄 이하)**
```
🔄 BEFORE (404줄 - 모든 로직이 한 클래스에 집중)
RoutineGrowthService
├── checkGrowthReadyRoutines()
├── checkAdaptiveRoutines() 
├── checkReductionReadyRoutines()
├── increaseRoutineTarget()
├── decreaseRoutineTarget()
├── resetGrowthCycle()
└── 다양한 validation 메서드들

✅ AFTER (기존 패키지 구조 유지)
domain/routine/service/
├── RoutineGrowthService.java           # 축소된 조정 관리자
├── GrowthAnalysisService.java          # 성장 분석 전문
├── ReductionAnalysisService.java       # 감소 분석 전문
└── strategy/
    ├── AdaptationStrategy.java         # 전략 인터페이스
    ├── GrowthStrategy.java             # 성장 전략 구현
    └── ReductionStrategy.java          # 감소 전략 구현
```

#### **1.2 DTO 통합 (21개 → 8개)**
```java
// ❌ BEFORE (중복적인 래핑 구조)
GrowthCheckResponse
AdaptiveRoutineCheckResponse  
DifficultyReductionCheckResponse
IncreaseTargetResponse
DecreaseTargetResponse
ResetGrowthCycleResponse

// ✅ AFTER (제네릭으로 통합)
RoutineAdaptationCheckResponse<T> {
    private List<T> candidates;
    private Integer totalCount;
    private AdaptationType type; // GROWTH, REDUCTION, MIXED
}

RoutineAdaptationResultResponse {
    private Long routineId;
    private Integer previousValue;
    private Integer newValue;
    private AdaptationAction action; // INCREASE, DECREASE, RESET
}
```

#### **1.3 기존 컨벤션 준수**
- **패키지 구조**: `domain/routine` 하위 구조 유지
- **CQRS 패턴**: `RoutineManagementFacade`, `RoutineQueryFacade` 역할 보존
- **테스트 호환성**: 기존 테스트 케이스 최대한 보존

---

### **Phase 2: 도메인 정제 및 CQRS 강화** ⏳ 2일 소요

#### **2.1 GrowthSettings 단순화**
```java
// ❌ BEFORE (196줄, 데이터 + 로직 + 검증 혼재)
@Embeddable GrowthSettings {
    // 너무 많은 책임
}

// ✅ AFTER (데이터 중심으로 단순화)
@Embeddable GrowthConfiguration {
    private Boolean isGrowthMode;
    private TargetType targetType;
    private Integer targetValue;
    private Integer targetIncrement;
    private Integer growthCycleDays;
    private Integer currentCycleDays;
    private LocalDate lastAdjustedDate;
    
    // 단순한 상태 확인 메서드만 유지
    public boolean isEnabled() { return Boolean.TRUE.equals(isGrowthMode); }
    public boolean isCycleCompleted() { return currentCycleDays >= growthCycleDays; }
}

// 비즈니스 로직은 서비스로 이동
class GrowthAnalysisService {
    public boolean canIncreaseTarget(RoutineEntity routine);
    public Integer calculateNextTarget(RoutineEntity routine);
}
```

#### **2.2 Facade 역할 강화**
```java
// RoutineController가 직접 새 서비스를 호출하지 않고
// 기존 Facade 패턴을 통해서만 접근하도록 개선

@Component
class RoutineManagementFacade {
    // 기존 코드 유지 + 새로운 서비스들 통합
    private final GrowthAnalysisService growthAnalysisService;
    private final ReductionAnalysisService reductionAnalysisService;
    
    public AdaptationResult executeAdaptation(Long routineId) {
        // Strategy 패턴으로 적절한 전략 선택 후 실행
    }
}
```

#### **2.3 CQRS 패턴 명확화**
- **Command (CUD)**: `RoutineManagementFacade` → 새로운 Analysis Services 호출
- **Query (R)**: `RoutineQueryFacade` → 기존 조회 로직 유지
- **컨트롤러**: Facade만 호출, 서비스 직접 접근 금지

---

### **Phase 3: API 일관성 확보 및 필수 이벤트 도입** ⏳ 2일 소요

#### **3.1 컨트롤러 통합 (RESTful 원칙 준수)**
```java
// ❌ BEFORE (3개 분산)
RoutineController + RoutineGrowthController + RoutineCategoryController

// ✅ AFTER (논리적 통합)
@RestController("/api/routines")
class RoutineController {
    // 기본 CRUD + 성장 관련 통합
    // 카테고리 조회도 포함
    
    private final RoutineManagementFacade managementFacade;
    private final RoutineQueryFacade queryFacade;
    
    // Facade 패턴을 통한 일관된 접근
}
```

#### **3.2 RESTful API 개선**
```
기존 API 유지 + 개선
GET    /api/routines                    # 내 루틴 목록
POST   /api/routines                    # 루틴 생성  
GET    /api/routines/{id}               # 루틴 상세
PUT    /api/routines/{id}               # 루틴 수정
DELETE /api/routines/{id}               # 루틴 삭제
GET    /api/routines/categories         # 카테고리 목록

개선된 적응 API (동사 제거)
GET    /api/routines/adaptation         # 적응 대상 조회
POST   /api/routines/{id}/target        # 목표 조정
  - Body: { "action": "INCREASE|DECREASE|RESET", "value": 10 }
```

#### **3.3 필수 도메인 이벤트 도입**
```java
// ApplicationEventPublisher 사용 (별도 Publisher 클래스 불필요)
@Component
class RoutineEntity {
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    public void increaseTarget() {
        Integer previousTarget = this.getTargetValue();
        this.growthSettings.increaseTarget();
        
        // 필수 이벤트만 발행
        eventPublisher.publishEvent(
            new RoutineTargetChangedEvent(routineId, previousTarget, getTargetValue())
        );
    }
}

// 이벤트 처리 (비동기)
@EventListener
@Async
class RoutineEventHandler {
    public void handleTargetChange(RoutineTargetChangedEvent event) {
        // 알림 발송, 통계 업데이트 등 필수 부수 효과만
        notificationService.sendAdaptationNotification(event);
    }
}
```

---


---

## 📊 **현실적 성과 예측**

### **Before → After 비교 (3단계 완료 시)**

| 항목 | Before | After | 개선율 |
|------|--------|--------|---------|
| **총 파일 수** | 27개 | 22개 | **-18%** |
| **DTO 개수** | 21개 | 10개 | **-52%** |  
| **가장 큰 클래스** | 404줄 | 150줄 | **-63%** |
| **컨트롤러 수** | 3개 | 1개 | **-67%** |
| **코드 중복도** | 높음 | 중간 | **-50%** |
| **평균 클래스 길이** | 180줄 | 120줄 | **-33%** |
| **개발 일정** | 10일 | **7일** | **-30%** |

### **핵심 개선 효과**

#### 🎯 **유지보수성 향상**
- **단일 책임 원칙**: RoutineGrowthService 분해로 클래스별 역할 명확화
- **코드 가독성**: 404줄 → 150줄 이하로 가독성 대폭 개선
- **테스트 용이성**: 작은 서비스 단위로 독립 테스트 가능
- **기존 컨벤션 준수**: `domain/routine` 구조 유지로 팀 혼란 방지

#### 🚀 **확장성 확보 (최소한)**
- **Strategy 패턴**: 새로운 적응 전략 추가 가능한 구조 마련
- **제네릭 DTO**: DTO 중복 제거로 새로운 응답 타입 빠른 생성
- **Facade 강화**: CQRS 패턴 통해 명령/조회 분리 명확화
- **이벤트 기반**: 필수 부수 효과만 비동기 처리

#### 🔒 **안정성 강화**
- **점진적 개선**: Big Bang 리팩토링 위험 제거
- **기존 API 호환**: 클라이언트 영향 최소화
- **테스트 호환성**: 기존 테스트 케이스 최대한 보존
- **리스크 관리**: 3단계 독립 검증으로 안전한 배포

#### 💡 **실용적 접근**
- **비즈니스 가치 중심**: 과잉 엔지니어링 배제
- **개발 일정 단축**: 10일 → 7일로 30% 단축
- **팀 학습 부담 최소화**: 복잡한 패턴 도입 제한
- **즉시 적용 가능**: 현재 팀 역량으로 바로 시작 가능

---

## ⏰ **현실적 실행 일정**

### **3단계 점진적 개선 (7일)**
```
Week 1: 핵심 문제 해결 (3일)
├── Day 1-2: RoutineGrowthService 분해 + Strategy 패턴
├── Day 3: DTO 통합 + 제네릭 패턴
└── 검증: 기존 테스트 호환성 확인

Week 2: 구조 정제 (2일)  
├── Day 1: GrowthSettings 단순화
├── Day 2: Facade 강화 + CQRS 명확화
└── 검증: 아키텍처 일관성 확인

Week 3: 최종 통합 (2일)
├── Day 1: API 통합 + RESTful 개선
├── Day 2: 필수 이벤트 도입 + 문서화
└── 검증: 전체 시스템 테스트
```

### **상세 마일스톤**

#### **Phase 1: 가장 시급한 문제 해결** (3일)
- **Day 1**: RoutineGrowthService 분석 및 GrowthAnalysisService, ReductionAnalysisService 생성
- **Day 2**: Strategy 패턴 인터페이스 구현 및 기존 로직 분산
- **Day 3**: DTO 통합 (21개 → 10개) 및 제네릭 패턴 적용

#### **Phase 2: 도메인 정제 및 CQRS 강화** (2일)
- **Day 1**: GrowthSettings 단순화 및 비즈니스 로직 서비스 이동
- **Day 2**: RoutineManagementFacade 강화 및 CQRS 패턴 명확화

#### **Phase 3: API 일관성 확보** (2일)
- **Day 1**: 컨트롤러 통합 및 RESTful API 개선
- **Day 2**: ApplicationEventPublisher 기반 이벤트 시스템 구축

**총 소요 기간**: **7일**  
**리스크**: 각 Phase별 독립 검증으로 최소화

---

## 🚨 **리스크 관리 (현실적 접근)**

### **Low Risk (해결됨)**
1. **기존 컨벤션 무시**: `domain/routine` 구조 유지로 해결
2. **과도한 복잡성**: Command, Factory 패턴 제거로 해결
3. **Big Bang 리팩토링**: 3단계 점진적 개선으로 해결
4. **팀 학습 부담**: 기존 지식 범위 내 패턴만 사용

### **Medium Risk**
1. **기존 API 호환성**: 컨트롤러 통합 시 URL 변경 가능성
2. **테스트 호환성**: 서비스 분해로 기존 테스트 수정 필요
3. **GrowthSettings 변경**: 임베디드 엔터티 구조 변경 시 주의 필요

### **High Risk (없음)**
- **데이터베이스 마이그레이션**: GrowthSettings 내부 구조만 변경, 스키마 불변
- **성능 저하**: 로직 분산만으로 성능 영향 없음
- **배포 복잡성**: 단계별 독립 배포로 복잡성 최소화

### **Mitigation Plan (간소화)**

#### **1. API 호환성 보장**
```java
// 기존 엔드포인트 유지, 내부 구현만 변경
@RestController("/api/routines/growth")  
class RoutineController {
    // 기존 URL 구조 유지
    // 내부에서만 새로운 서비스들 사용
}
```

#### **2. 테스트 단계별 검증**
```java
// Phase별 테스트 전략
Phase 1: 기존 RoutineGrowthService 테스트 → 새 서비스들로 점진적 이전
Phase 2: Facade 테스트 → CQRS 패턴 검증
Phase 3: API 테스트 → 전체 통합 시나리오 검증
```

#### **3. 롤백 준비**
- **Git Branch 전략**: Phase별 브랜치로 안전한 롤백
- **Feature Toggle**: 문제 발생 시 이전 로직으로 즉시 전환
- **모니터링**: 기본적인 성능/에러 지표만 추적

---

## 📚 **기술적 가이드라인**

### **1. 코딩 표준**
- **네이밍**: 도메인 용어 사용 (Growth → Adaptation, Check → Analyze)
- **메서드 길이**: 최대 20줄, 하나의 책임만 수행
- **클래스 길이**: 최대 200줄, SRP 준``수
- **주석**: 비즈니스 로직과 복잡한 알고리즘에만 작성

### **2. 아키텍처 원칙**
- **의존성 역전**: 상위 모듈이 하위 모듈에 의존하지 않음
- **인터페이스 분리**: 클라이언트가 사용하지 않는 인터페이스에 의존 금지
- **단일 책임**: 클래스 변경 이유는 단 하나
- **개방-폐쇄**: 확장에는 열려있고 수정에는 닫혀있음

### **3. 테스트 전략**
- **단위 테스트**: 각 클래스별 독립 테스트, 90% 이상 커버리지
- **통합 테스트**: API 엔드포인트별 시나리오 테스트
- **성능 테스트**: 응답시간 500ms 이하 유지
- **부하 테스트**: 동시 사용자 100명 처리 가능

---

## 🎉 **현실적 완료 후 기대 효과**

### **개발팀 관점**
1. **코드 리뷰 시간 30% 단축**: RoutineGrowthService 404줄 분해로 리뷰 용이성 향상
2. **버그 발생률 40% 감소**: 단일 책임 원칙 적용으로 오류 포인트 감소
3. **새 적응 전략 추가 시간 50% 단축**: Strategy 패턴으로 확장성 확보
4. **팀 혼란 최소화**: 기존 컨벤션 유지로 학습 부담 없음

### **비즈니스 관점**
1. **시스템 안정성**: 점진적 개선으로 서비스 중단 위험 없음
2. **확장성**: 새로운 루틴 적응 로직 추가 용이
3. **운영 비용**: 큰 변화 없이 코드 품질 개선
4. **개발 속도**: 명확한 책임 분리로 병렬 개발 가능

### **기술적 성과 (현실적)**
1. **코드 가독성**: 404줄 → 150줄 이하 (63% 개선)
2. **DTO 중복도**: 21개 → 10개 (52% 감소)
3. **컨트롤러 통합**: 3개 → 1개 (67% 감소)
4. **개발 기간**: 7일로 짧고 실현 가능한 목표

---

## 📋 **현실적 체크리스트**

### **Phase 1 완료 기준** (3일)
- [ ] RoutineGrowthService 404줄을 3개 서비스로 분해
    - [ ] GrowthAnalysisService 생성 및 성장 로직 이동
    - [ ] ReductionAnalysisService 생성 및 감소 로직 이동
    - [ ] Strategy 패턴 인터페이스 및 구현체 작성
- [ ] DTO 통합 (21개 → 10개)
    - [ ] 제네릭 AdaptationCheckResponse<T> 구현
    - [ ] 중복 Response 클래스 제거
- [ ] 기존 테스트 호환성 확보
- [ ] 패키지 구조 `domain/routine/service` 유지

### **Phase 2 완료 기준** (2일)
- [ ] GrowthSettings 단순화
    - [ ] 데이터 중심으로 재구성
    - [ ] 비즈니스 로직 서비스로 이동
- [ ] Facade 패턴 강화
    - [ ] RoutineManagementFacade에서 새 서비스들 통합 사용
    - [ ] CQRS 패턴 명확화 (Command/Query 분리)
- [ ] 기존 API 호환성 유지

### **Phase 3 완료 기준** (2일)
- [ ] 컨트롤러 통합 (3개 → 1개)
    - [ ] RoutineController에 growth 기능 통합
    - [ ] 기존 엔드포인트 URL 유지
- [ ] RESTful API 개선
    - [ ] 동사형 URL 제거
    - [ ] 리소스 중심 설계 적용
- [ ] ApplicationEventPublisher 기반 이벤트 시스템
    - [ ] 필수 도메인 이벤트만 선별적 도입
    - [ ] 비동기 처리 구현

### **최종 검증 기준**
- [ ] 모든 기존 기능 정상 작동
- [ ] 기존 테스트 케이스 95% 이상 통과
- [ ] RoutineGrowthService 150줄 이하로 축소
- [ ] 컨트롤러 통합 완료
- [ ] 패키지 구조 일관성 유지
- [ ] API 문서 업데이트

---

## 📖 **참고 문서**

### **설계 패턴 참고**
- [Strategy Pattern in Spring](https://springframework.guru/gang-of-four-design-patterns-in-spring-framework/)
- [Command Pattern for Undo Operations](https://refactoring.guru/design-patterns/command)
- [Domain Events with Spring](https://spring.io/blog/2017/01/30/what-s-new-in-spring-data-release-ingalls)

### **코드 품질 참고**
- [Clean Code Principles](https://clean-code-developer.com/)
- [SOLID Principles in Java](https://www.baeldung.com/solid-principles)
- [Refactoring Techniques](https://refactoring.guru/refactoring/techniques)

### **테스트 전략 참고**
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Test Pyramid Strategy](https://martinfowler.com/articles/practical-test-pyramid.html)

---

## 🔄 **Gemini 리뷰 반영 결과**

### **개선된 주요 변경사항**
1. **패키지 구조**: `adaptation/` 신설 제거 → `domain/routine/service` 구조 유지
2. **복잡성 감소**: Command, Factory 패턴 제거 → Strategy 패턴만 선별적 도입
3. **개발 기간**: 10일 → 7일로 단축 (30% 감소)
4. **리스크 관리**: Big Bang 리팩토링 → 점진적 3단계 개선
5. **실용성 강화**: 과잉 엔지니어링 배제 → 비즈니스 가치 중심 접근

### **Gemini 리뷰의 핵심 가치**
- **"무엇을 할 수 있는가" → "지금 무엇을 해야 하는가"**
- **기존 아키텍처 존중 및 강화**
- **80% 효과를 50% 노력으로 달성**
- **현실적이고 안전한 개선 경로 제시**

---

**문서 버전**: 2.0 (Gemini 리뷰 반영)  
**최종 수정일**: 2025-01-02  
**작성자**: Claude Code Assistant  
**리뷰어**: Gemini AI  
**개정 이유**: 과도한 엔지니어링 제거, 현실적 접근법 채택  
**승인자**: TBD