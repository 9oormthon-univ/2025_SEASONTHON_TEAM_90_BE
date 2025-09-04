# Daily Routine Snapshot 설계 문서

## 📋 개요

### 목표
사용자의 일일 루틴 기록에서 **완전성과 시간적 일관성**을 보장하기 위해, **기록 저장 시점에만** 해당 날짜의 모든 활성 루틴에 대한 스냅샷을 자동 생성하는 기능을 구현합니다.

### 해결하려는 문제
1. **불완전한 기록 표시**: 기록이 있는 날짜에서 기록되지 않은 루틴이 조회 시 누락됨
2. **시간적 불일치**: 해당 날짜 기준 루틴 목록 스냅샷이 없어 데이터 불일치 발생
3. **사용자 경험**: 기록 저장한 날짜의 모든 루틴 상태(수행/미수행)가 명확하게 표시되어야 함

### 기획 제약조건
- **당일만 수정 가능**: 사용자는 당일 루틴 기록만 수정할 수 있음
- **과거 기록 조회 제한**: 기록이 없는 과거 날짜는 "기록 없음" 상태로 처리

## 🎯 핵심 요구사항

### 기능 요구사항
1. **완전성**: 기록을 저장한 날짜의 모든 활성 루틴이 표시되어야 함
2. **시간적 일관성**: 기록 저장 시점의 루틴 목록 스냅샷 보존
3. **자동화**: 사용자 개입 없이 자동으로 스냅샷 생성
4. **데이터 보존**: 루틴 삭제 후에도 해당 날짜 기록은 유지

### 성능 요구사항
1. **저장 시점 생성**: 기록 저장 시에만 스냅샷 생성
2. **효율성**: 조회 시점의 불필요한 데이터 생성 방지
3. **확장성**: 사용자가 많은 루틴을 보유해도 처리 가능

## 🏗️ 아키텍처 설계

### A. 하이브리드 스냅샷 패턴

#### 스냅샷 생성/처리 시점
```
1. saveDailyRecord() 호출 시
   - 해당 날짜에 기록이 없으면 실제 스냅샷 생성 후 DB 저장 진행

2. getDailyRecord() 조회 시
   - 당일 조회이고 기록이 없으면 가상 NOT_PERFORMED 레코드 생성 (DB 저장 안함)
   - 과거 날짜는 기존 저장된 기록만 조회 (기록 없으면 빈 응답)
```

#### 장점
- ✅ 실제 저장하는 날짜만 DB에 데이터 생성하여 효율성 극대화
- ✅ 당일 UX 보장: 조회만 해도 완전한 루틴 상태 표시
- ✅ 불필요한 데이터 방지: 조회만 하고 저장 안 해도 DB 깔끔
- ✅ 과거 날짜 성능: 기존 데이터만 조회하여 빠른 응답

### B. 스냅샷 구성 요소

#### 스냅샷에 포함되는 루틴
- **대상**: 해당 날짜 기준 사용자의 모든 활성 루틴 (삭제되지 않은 루틴)
- **초기 상태**: `PerformanceLevel.NOT_PERFORMED`
- **데이터**: 루틴의 현재 스냅샷 정보 저장 (제목, 카테고리, 성장 설정 등)

#### 데이터 구조
```java
DailyRoutineEntity {
    routine: RoutineEntity (참조)
    performanceLevel: NOT_PERFORMED (초기값)
    consecutiveDays: 0 (초기값)
    performedDate: 해당 날짜
    // 루틴 스냅샷 필드들
    routineTitle, routineCategory, isGrowthMode, 
    targetType, targetValue, growthCycleDays, targetIncrement
}
```

## 🔧 구현 계획

### 1. API 설계

#### A. 새로운 API 엔드포인트
```java
// 1. 당일 루틴 현황 조회 (가상 레코드 포함)
GET /api/daily-records/today
Response: 항상 완전한 루틴 상태 반환 (기록 없으면 가상 NOT_PERFORMED)

// 2. 특정 날짜 기록 조회 (기존 데이터만)
GET /api/daily-records/{date}
Response: 실제 저장된 기록만 조회 (기록 없으면 빈 응답)

// 3. 당일 기록 저장 (기존 - 스냅샷 생성 로직 추가)
POST /api/daily-records/{date}
Request: SaveDailyRecordRequest
Response: 저장된 완전한 기록 상태
```

#### B. Controller 구현
```java
@RestController
@RequestMapping("/api/daily-records")
@RequiredArgsConstructor
public class DailyRecordController {
    
    private final DailyRecordFacade dailyRecordFacade;
    
    /**
     * 당일 루틴 현황 조회 (가상 NOT_PERFORMED 포함)
     */
    @GetMapping("/today")
    public CommonApiResponse<DailyRecordResponse> getTodayRecord(
            @AuthenticationPrincipal Long userId) {
        DailyRecordResponse response = dailyRecordFacade.getTodayRecordWithVirtual(userId);
        return CommonApiResponse.success(ApiSuccessCode.SUCCESS, response);
    }
    
    /**
     * 특정 날짜 기록 조회 (실제 저장된 데이터만)
     */
    @GetMapping("/{date}")
    public CommonApiResponse<DailyRecordResponse> getDailyRecord(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        DailyRecordResponse response = dailyRecordFacade.getDailyRecord(userId, date);
        return CommonApiResponse.success(ApiSuccessCode.SUCCESS, response);
    }
    
    /**
     * 일일 기록 저장 (스냅샷 생성 포함)
     */
    @PostMapping("/{date}")
    public CommonApiResponse<DailyRecordResponse> saveDailyRecord(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestBody @Valid SaveDailyRecordRequest request) {
        DailyRecordResponse response = dailyRecordFacade.saveDailyRecord(userId, date, request);
        return CommonApiResponse.success(ApiSuccessCode.CREATED, response);
    }
}
```

### 2. Repository 계층 확장

#### DailyRoutineRepository 추가 메서드
```java
/**
 * 특정 회원의 특정 날짜에 기록이 존재하는지 확인
 */
boolean existsByMemberIdAndPerformedDate(Long memberId, LocalDate date);

/**
 * 일괄 저장 (기존 JpaRepository.saveAll 활용)
 */
List<DailyRoutineEntity> saveAll(Iterable<DailyRoutineEntity> entities);
```

### 3. Service 계층 구현

#### A. DailyRecordFacade 확장
```java
@Component
@RequiredArgsConstructor
public class DailyRecordFacade {
    
    private final DailyRecordCommandService commandService;
    private final DailyRecordQueryService queryService;
    
    /**
     * 당일 기록 조회 (가상 레코드 포함)
     */
    @Transactional(readOnly = true)
    public DailyRecordResponse getTodayRecordWithVirtual(Long memberId) {
        LocalDate today = LocalDate.now();
        return queryService.getTodayRecordWithVirtual(memberId, today);
    }
    
    /**
     * 특정 날짜 기록 조회 (실제 데이터만)
     */
    @Transactional(readOnly = true)
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        return queryService.getDailyRecord(memberId, date);
    }
    
    /**
     * 일일 기록 저장 (스냅샷 생성 포함)
     */
    @Transactional
    public DailyRecordResponse saveDailyRecord(Long memberId, LocalDate date, SaveDailyRecordRequest request) {
        return commandService.saveDailyRecord(memberId, date, request);
    }
}
```

#### B. DailyRecordQueryService 확장
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyRecordQueryService {
    
    private final DailyReflectionService reflectionService;
    private final DailyRoutineService dailyRoutineService;
    private final RoutineService routineService;
    private final MemberRepository memberRepository;
    
    /**
     * 당일 기록 조회 (실제 기록 + 가상 NOT_PERFORMED 머지)
     */
    public DailyRecordResponse getTodayRecordWithVirtual(Long memberId, LocalDate today) {
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, today);
        
        // 1. 현재 모든 활성 루틴 조회
        List<RoutineEntity> allUserRoutines = routineService.getUserRoutines(memberId);
        
        // 2. 실제 기록된 데이터 조회
        List<DailyRoutineEntity> actualRecords = dailyRoutineService.getTodayRoutines(memberId, today);
        
        // 3. 실제 기록 + 가상 기록 머지
        List<DailyRoutineEntity> completeRecords = mergeActualAndVirtualRecords(
            allUserRoutines, actualRecords, memberId, today);
        
        return DailyRecordResponse.of(reflection.orElse(null), completeRecords);
    }
    
    /**
     * 특정 날짜 기록 조회 (실제 저장된 데이터만)
     */
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, date);
        List<DailyRoutineEntity> routineRecords = dailyRoutineService.getTodayRoutines(memberId, date);
        
        return DailyRecordResponse.of(reflection.orElse(null), routineRecords);
    }
    
    /**
     * 실제 기록과 가상 기록을 머지하여 완전한 루틴 상태 생성
     * 중간에 추가된 루틴도 즉시 NOT_PERFORMED 상태로 반영
     */
    private List<DailyRoutineEntity> mergeActualAndVirtualRecords(
            List<RoutineEntity> allUserRoutines, 
            List<DailyRoutineEntity> actualRecords,
            Long memberId, 
            LocalDate date) {
        
        // 이미 기록된 루틴 ID들 추출
        Set<Long> recordedRoutineIds = actualRecords.stream()
                .map(record -> record.getRoutine() != null ? record.getRoutine().getRoutineId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 기록되지 않은 루틴들을 가상 NOT_PERFORMED로 생성
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        List<DailyRoutineEntity> virtualRecords = allUserRoutines.stream()
                .filter(routine -> !recordedRoutineIds.contains(routine.getRoutineId()))
                .map(routine -> createVirtualNotPerformedRecord(routine, member, date))
                .collect(Collectors.toList());
        
        // 실제 기록 + 가상 기록 합치기
        List<DailyRoutineEntity> completeRecords = new ArrayList<>(actualRecords);
        completeRecords.addAll(virtualRecords);
        
        return completeRecords;
    }
    
    /**
     * 단일 가상 NOT_PERFORMED 레코드 생성
     */
    private DailyRoutineEntity createVirtualNotPerformedRecord(
            RoutineEntity routine, MemberEntity member, LocalDate date) {
        return DailyRoutineEntity.builder()
                .dailyRoutineId(null) // 가상 기록이므로 ID 없음
                .routine(routine)
                .member(member)
                .performanceLevel(PerformanceLevel.NOT_PERFORMED)
                .consecutiveDays(0)
                .performedDate(date)
                .routineTitle(routine.getTitle())
                .routineCategory(routine.getCategory())
                .isGrowthMode(routine.getIsGrowthMode())
                .targetType(routine.getTargetType())
                .targetValue(routine.getTargetValue())
                .growthCycleDays(routine.getGrowthCycleDays())
                .targetIncrement(routine.getTargetIncrement())
                .build();
    }
}
```

#### C. DailyRecordDomainService 확장
```java
@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecordDomainService {
    
    private final RoutineService routineService;
    private final MemberRepository memberRepository;
    private final DailyRoutineService dailyRoutineService;
    private final DailyRoutineRepository dailyRoutineRepository;
    
    /**
     * 스냅샷 존재 보장 (공통 메서드)
     */
    public void ensureDailyRoutineSnapshotExists(Long memberId, LocalDate date) {
        if (!dailyRoutineRepository.existsByMemberIdAndPerformedDate(memberId, date)) {
            createInitialSnapshot(memberId, date);
        }
    }
    
    /**
     * 초기 스냅샷 생성 (실제 DB 저장)
     */
    private void createInitialSnapshot(Long memberId, LocalDate date) {
        List<RoutineEntity> activeRoutines = routineService.getUserRoutines(memberId);
        if (activeRoutines.isEmpty()) {
            return; // 루틴이 없으면 스냅샷 생성하지 않음
        }
        
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        List<DailyRoutineEntity> initialRecords = activeRoutines.stream()
                .map(routine -> DailyRoutineEntity.create(routine, member, 
                    PerformanceLevel.NOT_PERFORMED, date, 0))
                .collect(Collectors.toList());
        
        dailyRoutineRepository.saveAll(initialRecords);
        
        log.info("Created daily routine snapshot for member: {} on date: {} with {} routines", 
                memberId, date, initialRecords.size());
    }
    
    // 기존 메서드들...
    public List<DailyRoutineEntity> saveRoutineRecords(Long memberId, LocalDate date, 
                                                      List<RoutineRecordRequest> routineRecords) {
        if (routineRecords == null || routineRecords.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RoutinePerformanceRequest> enrichedRecords = enrichRoutineRecords(routineRecords, memberId);
        return dailyRoutineService.saveRoutineRecords(memberId, date, enrichedRecords);
    }
    
    // 기존 검증 메서드들 유지...
}
```

#### D. DailyRecordCommandService 수정
```java
@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecordCommandService {
    
    private final DailyRecordDomainService domainService;
    private final DailyReflectionService reflectionService;
    
    public DailyRecordResponse saveDailyRecord(Long memberId, LocalDate date, SaveDailyRecordRequest request) {
        domainService.validateDateModifiable(date);
        
        // 스냅샷 보장 (새 로직 - 첫 저장 시 실제 DB 저장)
        domainService.ensureDailyRoutineSnapshotExists(memberId, date);
        
        DailyReflectionEntity reflection = saveReflectionIfPresent(memberId, date, request);
        List<DailyRoutineEntity> routineRecords = domainService.saveRoutineRecords(
            memberId, date, request.getRoutineRecords());
        
        return DailyRecordResponse.of(reflection, routineRecords);
    }
    
    private DailyReflectionEntity saveReflectionIfPresent(Long memberId, LocalDate date, 
                                                         SaveDailyRecordRequest request) {
        if (request.getReflection() == null) {
            return null;
        }
        
        return reflectionService.saveReflection(
            memberId, date, 
            request.getReflection().getContent(),
            request.getReflection().getEmotion()
        );
    }
}
```

## 🔄 동작 플로우

### A. 저장 시나리오
```
1. 사용자가 2024-01-15 루틴 기록 저장 요청
2. ensureDailyRoutineSnapshotExists(memberId, 2024-01-15) 호출
3. 해당 날짜 기록 존재 확인
   - 기록 없음 → 현재 활성 루틴 5개에 대해 NOT_PERFORMED을 DB에 실제 저장
   - 기록 있음 → 스킵
4. 실제 기록 업데이트 (운동A: FULL_SUCCESS, 독서B: PARTIAL_SUCCESS)
5. 최종 결과: 5개 루틴 모두 적절한 상태로 DB 저장
```

### B. 당일 조회 시나리오 (머지 방식)
```
1. 사용자가 오늘 날짜(2024-01-15) 루틴 기록 조회 요청
2. 현재 활성 루틴 3개 조회 (운동A, 독서B, 명상C)
3. 실제 기록 확인 → 운동A만 FULL_SUCCESS로 기록됨
4. 머지 로직 실행:
   - 운동A: 실제 기록 (FULL_SUCCESS)
   - 독서B, 명상C: 가상 기록 (NOT_PERFORMED) 
5. 사용자는 3개 루틴 모두의 상태 확인 (1개 완료, 2개 미수행)
```

### C. 과거 날짜 조회 시나리오
```
1. 사용자가 2024-01-10 루틴 기록 조회 요청
2. 해당 날짜 실제 기록만 조회
3. 기록이 있으면 → 저장된 스냅샷 반환
4. 기록이 없으면 → 빈 응답 반환 ("기록 없음" UI)
```

## 📊 예상 영향도

### 데이터베이스
- **저장 공간**: 실제 사용하는 날짜만 기록하므로 적정 수준
- **쿼리 성능**: 단순 존재 여부 확인 + 일괄 삽입으로 최적화
- **인덱스**: `(member_id, performed_date)` 복합 인덱스 활용

### 성능
- **초기 지연**: 첫 조회/저장 시 스냅샷 생성으로 약간의 지연 발생
- **후속 처리**: 두 번째 호출부터는 기존과 동일한 성능
- **메모리**: 루틴 수에 비례하는 임시 객체 생성

### 사용자 경험
- **일관성**: 모든 날짜에서 완전한 루틴 상태 확인 가능
- **직관성**: 명시적으로 미수행 상태 표시
- **히스토리**: 과거 시점의 정확한 루틴 목록 보존

## 🧪 테스트 시나리오

### 1. 기본 시나리오
- [ ] 빈 날짜에 첫 기록 저장 시 전체 스냅샷 생성 확인
- [ ] 기존 기록이 있는 날짜 추가 저장 시 스냅샷 생성 스킵 확인
- [ ] 기록 없는 날짜 조회 시 스냅샷 생성 후 조회 확인

### 2. 에지 케이스
- [ ] 루틴이 없는 사용자의 기록 저장/조회
- [ ] 과거 날짜와 현재 날짜의 루틴 목록 차이 확인
- [ ] 루틴 삭제 후 과거 기록 조회 시 데이터 보존 확인

### 3. 성능 테스트
- [ ] 다수 루틴 보유 사용자의 스냅샷 생성 시간 측정
- [ ] 동시 다발적 날짜별 조회/저장 성능 확인

## 🚀 구현 단계별 체크리스트

### 1단계: Repository 계층 구현
- [ ] `DailyRoutineRepository.existsByMemberIdAndPerformedDate()` 메서드 추가
- [ ] Repository 단위 테스트 작성 및 실행

### 2단계: Domain Service 구현
- [ ] `DailyRecordDomainService.ensureDailyRoutineSnapshotExists()` 구현
- [ ] `DailyRecordDomainService.createInitialSnapshot()` 구현
- [ ] Domain Service 단위 테스트 작성

### 3단계: Query Service 확장
- [ ] `DailyRecordQueryService.getTodayRecordWithVirtual()` 구현 (머지 방식)
- [ ] `DailyRecordQueryService.mergeActualAndVirtualRecords()` 구현
- [ ] 가상 레코드 생성 및 머지 로직 테스트

### 4단계: Command Service 수정
- [ ] `DailyRecordCommandService.saveDailyRecord()` 스냅샷 로직 추가
- [ ] 기존 저장 로직과 통합 테스트

### 5단계: Facade 계층 확장
- [ ] `DailyRecordFacade.getTodayRecordWithVirtual()` 구현
- [ ] 트랜잭션 경계 설정 확인

### 6단계: Controller API 구현
- [ ] `GET /api/daily-records/today` 엔드포인트 구현
- [ ] 기존 `GET /api/daily-records/{date}` 동작 확인
- [ ] `POST /api/daily-records/{date}` 스냅샷 로직 통합

### 7단계: 통합 테스트
- [ ] **당일 첫 조회** → 모든 루틴 NOT_PERFORMED 상태 반환 확인
- [ ] **당일 부분 기록 후 조회** → 실제 기록 + 가상 기록 머지 확인
- [ ] **당일 루틴 추가 후 조회** → 새 루틴 즉시 NOT_PERFORMED 반영 확인
- [ ] **당일 첫 저장** → 실제 스냅샷 생성 확인
- [ ] **과거 날짜 조회** → 기존 데이터만 반환 확인
- [ ] **빈 과거 날짜 조회** → 빈 응답 반환 확인

### 8단계: API 문서 업데이트
- [ ] Swagger 문서 업데이트
- [ ] API 명세서 업데이트 (`/api-docs/DAILY_API_SPECIFICATION.md`)

## 🧪 핵심 테스트 시나리오

### A. 당일 시나리오 테스트
```java
@Test
void 당일_조회_시_루틴없으면_가상레코드_반환() {
    // Given: 오늘 날짜, 루틴 3개 존재, 기록 없음
    // When: GET /api/daily-records/today 호출
    // Then: 3개 루틴 모두 NOT_PERFORMED 상태로 반환, DB 저장 안됨
}

@Test
void 당일_첫저장_시_스냅샷_생성() {
    // Given: 오늘 날짜, 루틴 3개 존재, 기록 없음
    // When: POST /api/daily-records/2024-01-15 호출
    // Then: 3개 루틴 NOT_PERFORMED 스냅샷 DB 저장, 실제 기록 업데이트
}
```

### B. 과거 날짜 시나리오 테스트
```java
@Test
void 과거날짜_조회_시_기존데이터만_반환() {
    // Given: 과거 날짜, 저장된 기록 존재
    // When: GET /api/daily-records/2024-01-10 호출
    // Then: 실제 저장된 기록만 반환
}

@Test
void 과거날짜_빈조회_시_빈응답_반환() {
    // Given: 과거 날짜, 저장된 기록 없음
    // When: GET /api/daily-records/2024-01-05 호출
    // Then: 빈 routineRecords 반환
}
```

### C. 에지 케이스 테스트
```java
@Test
void 루틴없는_사용자_당일조회() {
    // Given: 사용자에게 루틴 없음
    // When: GET /api/daily-records/today 호출
    // Then: 빈 routineRecords 반환
}

@Test
void 루틴_중간추가_후_당일조회() {
    // Given: 오전에 루틴A 기록 저장, 오후에 새 루틴B 추가
    // When: GET /api/daily-records/today 호출
    // Then: 루틴A(실제 기록) + 루틴B(가상 NOT_PERFORMED) 모두 반환
}

@Test
void 당일_부분기록_후_조회() {
    // Given: 루틴 3개 존재, 1개만 기록 저장
    // When: GET /api/daily-records/today 호출
    // Then: 1개 실제 기록 + 2개 가상 NOT_PERFORMED 반환
}
```

## 🔍 성능 고려사항

### 최적화 포인트
1. **Repository 쿼리**: `existsByMemberIdAndPerformedDate` 인덱스 최적화
2. **가상 레코드 생성**: 메모리 효율적인 Stream 처리
3. **트랜잭션 경계**: Command/Query 분리로 읽기 성능 보장

### 모니터링 메트릭
- 스냅샷 생성 빈도 및 소요 시간
- 가상 레코드 생성 성능
- API 응답 시간 (특히 `/today` 엔드포인트)

---

**최종 수정일**: 2025-01-09  
**구현 담당**: Development Team  
**검토 상태**: Ready for Implementation  
**예상 구현 시간**: 1-2일