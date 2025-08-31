# 일일 루틴 관리 시스템 - 최종 요구사항 및 구현 계획

## 1. 최종 요구사항 정의

### 1.1 핵심 기능
- **일일 루틴 수행 기록 관리**: 유저별 루틴의 일별 수행 상태 추적
- **연속 수행 일수 추적**: 완전성공 기준 연속 달성 일수 자동 계산
- **일별 회고 시스템**: 감정 상태와 텍스트 회고를 통한 하루 기록
- **통합 UI**: 단일 페이지에서 루틴 수행 + 회고 동시 관리

### 1.2 비즈니스 규칙
#### 루틴 수행 기록
- **수행 상태**: 완전성공, 부분성공, 미수행 (3단계)
- **연속성 계산**: 완전성공만 연속으로 인정, 부분성공/미수행 시 0으로 초기화
- **수정 권한**: 당일 데이터만 수정 가능 (과거/미래 불가)
- **기본값**: 미수행 상태로 초기화
- **데이터 보존**: 루틴 삭제되어도 수행 기록은 영구 보존

#### 회고 시스템
- **회고 단위**: 유저별 일일 단위 (하루 하나의 회고)
- **감정 상태**: HAPPY, SOSO, SAD, MAD (4가지)
- **수정 권한**: 당일만 수정 가능
- **필수 여부**: 회고 내용과 감정 모두 선택적 입력

#### UI/UX 동작
- **통합 저장**: 루틴 수행 기록 + 회고를 단일 API로 저장
- **실시간 반영**: 회고 작성 중 추가된 새 루틴 즉시 표시
- **상태 관리**: 페이지 새로고침 시에도 당일 작성 내용 유지

## 2. 데이터베이스 설계

### 2.1 Entity 구조

#### DailyRoutineEntity
```sql
CREATE TABLE daily_routine_table (
    daily_routine_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    routine_id BIGINT NULL,  -- 루틴 삭제 대비 NULL 허용
    member_id BIGINT NOT NULL,
    performance_level VARCHAR(20) NOT NULL,  -- FULL_SUCCESS, PARTIAL_SUCCESS, NOT_PERFORMED
    consecutive_days INTEGER NOT NULL DEFAULT 0,
    performed_date DATE NOT NULL,
    
    -- 루틴 삭제 대비 스냅샷 데이터
    routine_title VARCHAR(100),
    routine_category VARCHAR(50),
    
    -- 성장 모드 스냅샷 데이터
    is_growth_mode BOOLEAN,
    target_type VARCHAR(20),
    target_value INTEGER,
    growth_cycle_days INTEGER,
    target_increment INTEGER,
    
    -- BaseTimeEntity에서 자동 관리되므로 SQL DDL에서 제거
    -- created_at, updated_at은 @CreatedDate, @LastModifiedDate로 관리
    
    FOREIGN KEY (routine_id) REFERENCES routine_table(routine_id),
    FOREIGN KEY (member_id) REFERENCES member_table(id),
    
    -- 복합 인덱스
    INDEX idx_daily_routine_member_date (member_id, performed_date),
    INDEX idx_daily_routine_routine_date (routine_id, performed_date),
    
    -- 유니크 제약 조건
    UNIQUE KEY uk_member_routine_date (member_id, routine_id, performed_date)
);
```

#### DailyReflectionEntity
```sql
CREATE TABLE daily_reflection_table (
    reflection_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    reflection_content TEXT,
    emotion VARCHAR(10) NOT NULL,  -- HAPPY, SOSO, SAD, MAD
    reflection_date DATE NOT NULL,
    
    -- BaseTimeEntity에서 자동 관리되므로 SQL DDL에서 제거
    -- created_at, updated_at은 @CreatedDate, @LastModifiedDate로 관리
    
    FOREIGN KEY (member_id) REFERENCES member_table(id),
    
    -- 인덱스
    INDEX idx_reflection_member_date (member_id, reflection_date),
    
    -- 유니크 제약 조건
    UNIQUE KEY uk_member_reflection_date (member_id, reflection_date)
);
```

### 2.2 Enum 클래스 정의
```java
public enum PerformanceLevel {
    FULL_SUCCESS("완전성공"),
    PARTIAL_SUCCESS("부분성공"), 
    NOT_PERFORMED("미수행");
}

public enum EmotionType {
    HAPPY("행복"),
    SOSO("그저그래"),
    SAD("슬픔"),
    MAD("화남");
}
```

## 3. 구체적 구현 계획

### 3.1 1단계: Core Entity 구현 (1일차)

#### 파일 구조
```
src/main/java/com/groomthon/habiglow/domain/daily/
├── entity/
│   ├── DailyRoutineEntity.java
│   ├── DailyReflectionEntity.java
│   ├── PerformanceLevel.java
│   └── EmotionType.java
└── ...
```

#### DailyRoutineEntity.java
```java
@Entity
@Table(name = "daily_routine_table", 
       indexes = {
           @Index(name = "idx_daily_routine_member_date", columnList = "member_id, performed_date"),
           @Index(name = "idx_daily_routine_routine_date", columnList = "routine_id, performed_date")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_member_routine_date", columnNames = {"member_id", "routine_id", "performed_date"})
       })
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
public class DailyRoutineEntity extends BaseTimeEntity {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dailyRoutineId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = true)
    private RoutineEntity routine;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "performance_level", nullable = false)
    private PerformanceLevel performanceLevel;
    
    @Column(name = "consecutive_days", nullable = false)
    @Builder.Default
    private Integer consecutiveDays = 0;
    
    @Column(name = "performed_date", nullable = false)
    private LocalDate performedDate;
    
    // 루틴 삭제 대비 스냅샷
    @Column(name = "routine_title", length = 100)
    private String routineTitle;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "routine_category")
    private RoutineCategory routineCategory;
    
    // 비즈니스 메서드
    public void updatePerformance(PerformanceLevel performanceLevel, Integer consecutiveDays) {
        this.performanceLevel = performanceLevel;
        this.consecutiveDays = consecutiveDays;
    }
    
    public boolean isFullSuccess() {
        return this.performanceLevel == PerformanceLevel.FULL_SUCCESS;
    }
    
    // 팩토리 메서드
    public static DailyRoutineEntity create(RoutineEntity routine, MemberEntity member, 
                                           PerformanceLevel performance, LocalDate date, Integer consecutiveDays) {
        return DailyRoutineEntity.builder()
                .routine(routine)
                .member(member)
                .performanceLevel(performance)
                .performedDate(date)
                .consecutiveDays(consecutiveDays)
                .routineTitle(routine.getTitle())
                .routineCategory(routine.getCategory())
                .build();
    }
}
```

### 3.2 2단계: Repository 구현 (1일차)

#### DailyRoutineRepository.java
```java
public interface DailyRoutineRepository extends JpaRepository<DailyRoutineEntity, Long> {
    
    // 특정 유저의 특정 날짜 모든 루틴 수행 기록
    @Query("SELECT dr FROM DailyRoutineEntity dr " +
           "LEFT JOIN FETCH dr.routine " +
           "WHERE dr.member.id = :memberId AND dr.performedDate = :date")
    List<DailyRoutineEntity> findByMemberIdAndPerformedDateWithRoutine(@Param("memberId") Long memberId, 
                                                                       @Param("date") LocalDate date);
    
    // 특정 루틴의 어제 수행 기록 (연속성 계산용)
    Optional<DailyRoutineEntity> findByRoutineRoutineIdAndMemberIdAndPerformedDate(
        Long routineId, Long memberId, LocalDate date);
    
    // 특정 유저+루틴+날짜 기록 존재 여부 확인
    boolean existsByMemberIdAndRoutineRoutineIdAndPerformedDate(Long memberId, Long routineId, LocalDate date);
    
    // 벌크 저장을 위한 삭제
    @Modifying
    @Query("DELETE FROM DailyRoutineEntity dr WHERE dr.member.id = :memberId AND dr.performedDate = :date")
    void deleteByMemberIdAndPerformedDate(@Param("memberId") Long memberId, @Param("date") LocalDate date);
}
```

#### DailyReflectionRepository.java
```java
public interface DailyReflectionRepository extends JpaRepository<DailyReflectionEntity, Long> {
    
    // 특정 유저의 특정 날짜 회고
    Optional<DailyReflectionEntity> findByMemberIdAndReflectionDate(Long memberId, LocalDate date);
    
    // 특정 날짜 회고 존재 여부
    boolean existsByMemberIdAndReflectionDate(Long memberId, LocalDate date);
}
```

### 3.3 3단계: Service Layer 구현 (2일차)

#### ConsecutiveDaysCalculator.java
```java
@Component
@RequiredArgsConstructor
public class ConsecutiveDaysCalculator {
    
    private final DailyRoutineRepository dailyRoutineRepository;
    
    public int calculate(Long routineId, Long memberId, LocalDate currentDate, PerformanceLevel performance) {
        
        // 완전성공이 아니면 연속성 끊김
        if (performance != PerformanceLevel.FULL_SUCCESS) {
            return 0;
        }
        
        // 어제 기록 확인
        LocalDate yesterday = currentDate.minusDays(1);
        Optional<DailyRoutineEntity> yesterdayRecord = dailyRoutineRepository
            .findByRoutineRoutineIdAndMemberIdAndPerformedDate(routineId, memberId, yesterday);
        
        // 어제 기록이 없거나 완전성공이 아니면 연속성 새로 시작
        if (yesterdayRecord.isEmpty() || !yesterdayRecord.get().isFullSuccess()) {
            return 1;
        }
        
        // 어제 완전성공 -> 연속성 이어짐
        return yesterdayRecord.get().getConsecutiveDays() + 1;
    }
}
```

#### DailyRoutineService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class DailyRoutineService {
    
    private final DailyRoutineRepository dailyRoutineRepository;
    private final ConsecutiveDaysCalculator consecutiveDaysCalculator;
    
    public List<DailyRoutineEntity> saveRoutineRecords(Long memberId, LocalDate date, 
                                                      List<RoutinePerformanceRequest> records) {
        
        // 기존 당일 기록 모두 삭제 (upsert 방식)
        dailyRoutineRepository.deleteByMemberIdAndPerformedDate(memberId, date);
        
        List<DailyRoutineEntity> entities = new ArrayList<>();
        
        for (RoutinePerformanceRequest record : records) {
            // 연속 일수 계산
            int consecutiveDays = consecutiveDaysCalculator.calculate(
                record.getRoutineId(), memberId, date, record.getPerformanceLevel());
            
            // 엔터티 생성
            DailyRoutineEntity entity = DailyRoutineEntity.create(
                record.getRoutine(), // RoutineService에서 조회 필요
                record.getMember(),  // MemberService에서 조회 필요
                record.getPerformanceLevel(),
                date,
                consecutiveDays
            );
            
            entities.add(entity);
        }
        
        return dailyRoutineRepository.saveAll(entities);
    }
    
    @Transactional(readOnly = true)
    public List<DailyRoutineEntity> getTodayRoutines(Long memberId, LocalDate date) {
        return dailyRoutineRepository.findByMemberIdAndPerformedDateWithRoutine(memberId, date);
    }
}
```

#### DailyReflectionService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class DailyReflectionService {
    
    private final DailyReflectionRepository reflectionRepository;
    
    public DailyReflectionEntity saveReflection(Long memberId, LocalDate date, 
                                               String content, EmotionType emotion) {
        
        Optional<DailyReflectionEntity> existing = reflectionRepository
            .findByMemberIdAndReflectionDate(memberId, date);
        
        if (existing.isPresent()) {
            // 기존 회고 수정
            DailyReflectionEntity entity = existing.get();
            entity.updateReflection(content, emotion);
            return entity;
        }
        
        // 새 회고 생성
        DailyReflectionEntity entity = DailyReflectionEntity.create(memberId, content, emotion, date);
        return reflectionRepository.save(entity);
    }
    
    @Transactional(readOnly = true)
    public Optional<DailyReflectionEntity> getReflection(Long memberId, LocalDate date) {
        return reflectionRepository.findByMemberIdAndReflectionDate(memberId, date);
    }
}
```

### 3.4 4단계: Facade Layer 구현 (3일차)

#### DailyRecordFacade.java
```java
@Component
@RequiredArgsConstructor
@Transactional
public class DailyRecordFacade {
    
    private final DailyRoutineService dailyRoutineService;
    private final DailyReflectionService reflectionService;
    private final RoutineService routineService;
    private final MemberService memberService;
    
    public DailyRecordResponse saveDailyRecord(Long memberId, LocalDate date, SaveDailyRecordRequest request) {
        
        // 당일 데이터만 수정 가능 검증
        validateDateModifiable(date);
        
        // 회고 저장
        DailyReflectionEntity reflection = null;
        if (request.getReflection() != null) {
            reflection = reflectionService.saveReflection(
                memberId, date, 
                request.getReflection().getContent(),
                request.getReflection().getEmotion()
            );
        }
        
        // 루틴 수행 기록 저장
        List<DailyRoutineEntity> routineRecords = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getRoutineRecords())) {
            // 루틴 정보와 멤버 정보 조회하여 엔티티 생성 준비
            List<RoutinePerformanceRequest> enrichedRecords = enrichRoutineRecords(
                request.getRoutineRecords(), memberId);
            
            routineRecords = dailyRoutineService.saveRoutineRecords(memberId, date, enrichedRecords);
        }
        
        return DailyRecordResponse.of(reflection, routineRecords);
    }
    
    @Transactional(readOnly = true)
    public DailyRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        
        // 회고 조회
        Optional<DailyReflectionEntity> reflection = reflectionService.getReflection(memberId, date);
        
        // 루틴 수행 기록 조회
        List<DailyRoutineEntity> routineRecords = dailyRoutineService.getTodayRoutines(memberId, date);
        
        // 유저의 모든 루틴 조회 (미수행 루틴도 표시하기 위해)
        List<RoutineEntity> allUserRoutines = routineService.getUserRoutines(memberId);
        
        return DailyRecordResponse.of(reflection.orElse(null), routineRecords, allUserRoutines);
    }
    
    private void validateDateModifiable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (!date.equals(today)) {
            throw new IllegalArgumentException("당일 데이터만 수정 가능합니다.");
        }
    }
    
    private List<RoutinePerformanceRequest> enrichRoutineRecords(
            List<RoutineRecordRequest> records, Long memberId) {
        
        MemberEntity member = memberService.findById(memberId);
        
        // N+1 문제 해결: 모든 routineId를 한 번에 수집
        Set<Long> routineIds = records.stream()
            .map(RoutineRecordRequest::getRoutineId)
            .collect(Collectors.toSet());
        
        // 단일 쿼리로 모든 루틴 조회
        Map<Long, RoutineEntity> routineMap = routineService.findAllByIds(routineIds)
            .stream()
            .collect(Collectors.toMap(RoutineEntity::getRoutineId, Function.identity()));
        
        // Map에서 조회하여 변환
        return records.stream()
            .map(record -> {
                RoutineEntity routine = routineMap.get(record.getRoutineId());
                if (routine == null) {
                    throw new RoutineNotFoundException("루틴을 찾을 수 없습니다: " + record.getRoutineId());
                }
                return RoutinePerformanceRequest.of(routine, member, record.getPerformanceLevel());
            })
            .collect(Collectors.toList());
    }
}
```

### 3.5 5단계: DTO 구현 (3일차)

#### Request DTOs
```java
@Getter @NoArgsConstructor @AllArgsConstructor
public class SaveDailyRecordRequest {
    private ReflectionRequest reflection;
    private List<RoutineRecordRequest> routineRecords;
    
    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class ReflectionRequest {
        private String content;
        private EmotionType emotion;
    }
    
    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class RoutineRecordRequest {
        private Long routineId;
        private PerformanceLevel performanceLevel;
    }
}
```

#### Response DTOs
```java
@Getter @AllArgsConstructor
public class DailyRecordResponse {
    private ReflectionResponse reflection;
    private List<RoutineRecordResponse> routineRecords;
    private List<RoutineResponse> allRoutines; // UI에서 미수행 루틴 표시용
    
    public static DailyRecordResponse of(DailyReflectionEntity reflection, 
                                        List<DailyRoutineEntity> records,
                                        List<RoutineEntity> allRoutines) {
        
        ReflectionResponse reflectionDto = reflection != null ? 
            ReflectionResponse.from(reflection) : null;
        
        List<RoutineRecordResponse> recordDtos = records.stream()
            .map(RoutineRecordResponse::from)
            .collect(Collectors.toList());
        
        List<RoutineResponse> routineDtos = allRoutines.stream()
            .map(RoutineResponse::from)
            .collect(Collectors.toList());
        
        return new DailyRecordResponse(reflectionDto, recordDtos, routineDtos);
    }
}

@Getter @AllArgsConstructor
public class RoutineRecordResponse {
    private Long routineId;
    private String routineTitle;
    private RoutineCategory category;
    private PerformanceLevel performanceLevel;
    private Integer consecutiveDays;
    
    public static RoutineRecordResponse from(DailyRoutineEntity entity) {
        return new RoutineRecordResponse(
            entity.getRoutine() != null ? entity.getRoutine().getRoutineId() : null,
            entity.getRoutineTitle(),
            entity.getRoutineCategory(),
            entity.getPerformanceLevel(),
            entity.getConsecutiveDays()
        );
    }
}
```

### 3.6 6단계: Controller 구현 (4일차)

#### DailyRecordController.java
```java
@RestController
@RequestMapping("/api/daily-records")
@RequiredArgsConstructor
public class DailyRecordController {
    
    private final DailyRecordFacade dailyRecordFacade;
    
    // 일일 기록 저장 (루틴 수행 + 회고)
    @PostMapping("/{date}")
    public CommonApiResponse<DailyRecordResponse> saveDailyRecord(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestBody @Valid SaveDailyRecordRequest request,
            Authentication authentication) {
        
        Long memberId = extractMemberId(authentication);
        DailyRecordResponse response = dailyRecordFacade.saveDailyRecord(memberId, date, request);
        
        return CommonApiResponse.success(response);
    }
    
    // 특정 날짜 기록 조회
    @GetMapping("/{date}")
    public CommonApiResponse<DailyRecordResponse> getDailyRecord(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            Authentication authentication) {
        
        Long memberId = extractMemberId(authentication);
        DailyRecordResponse response = dailyRecordFacade.getDailyRecord(memberId, date);
        
        return CommonApiResponse.success(response);
    }
    
    // 오늘 기록 조회 (편의 API)
    @GetMapping("/today")
    public CommonApiResponse<DailyRecordResponse> getTodayRecord(Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        DailyRecordResponse response = dailyRecordFacade.getDailyRecord(memberId, LocalDate.now());
        
        return CommonApiResponse.success(response);
    }
    
    private Long extractMemberId(Authentication authentication) {
        return ((CustomUserPrincipal) authentication.getPrincipal()).getId();
    }
}
```

### 3.7 7단계: 테스트 구현 (5일차)

#### 통합 테스트
```java
@SpringBootTest
@Transactional
class DailyRecordIntegrationTest {
    
    @Autowired private DailyRecordFacade dailyRecordFacade;
    @Autowired private TestDataFactory testDataFactory;
    
    @Test
    void 일일기록_저장_및_조회_성공() {
        // Given
        MemberEntity member = testDataFactory.createMember();
        RoutineEntity routine1 = testDataFactory.createRoutine(member, "물 8잔 마시기");
        RoutineEntity routine2 = testDataFactory.createRoutine(member, "영어 단어 20개");
        
        SaveDailyRecordRequest request = SaveDailyRecordRequest.builder()
            .reflection(new ReflectionRequest("오늘은 좋은 하루였다", EmotionType.HAPPY))
            .routineRecords(Arrays.asList(
                new RoutineRecordRequest(routine1.getRoutineId(), PerformanceLevel.FULL_SUCCESS),
                new RoutineRecordRequest(routine2.getRoutineId(), PerformanceLevel.PARTIAL_SUCCESS)
            ))
            .build();
        
        LocalDate today = LocalDate.now();
        
        // When
        DailyRecordResponse response = dailyRecordFacade.saveDailyRecord(member.getId(), today, request);
        
        // Then
        assertThat(response.getReflection().getContent()).isEqualTo("오늘은 좋은 하루였다");
        assertThat(response.getReflection().getEmotion()).isEqualTo(EmotionType.HAPPY);
        assertThat(response.getRoutineRecords()).hasSize(2);
        
        // 연속 일수 검증
        RoutineRecordResponse routine1Record = response.getRoutineRecords().stream()
            .filter(r -> r.getRoutineId().equals(routine1.getRoutineId()))
            .findFirst().orElseThrow();
        assertThat(routine1Record.getConsecutiveDays()).isEqualTo(1); // 완전성공
        
        RoutineRecordResponse routine2Record = response.getRoutineRecords().stream()
            .filter(r -> r.getRoutineId().equals(routine2.getRoutineId()))
            .findFirst().orElseThrow();
        assertThat(routine2Record.getConsecutiveDays()).isEqualTo(0); // 부분성공 -> 연속성 끊김
    }
    
    @Test
    void 연속_수행_일수_정확히_계산됨() {
        // 2일 연속 완전성공 -> 3일째 부분성공 -> 4일째 완전성공 시나리오 테스트
    }
    
    @Test
    void 과거_날짜_수정_시_예외_발생() {
        // Given
        MemberEntity member = testDataFactory.createMember();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SaveDailyRecordRequest request = new SaveDailyRecordRequest();
        
        // When & Then
        assertThatThrownBy(() -> 
            dailyRecordFacade.saveDailyRecord(member.getId(), yesterday, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("당일 데이터만 수정 가능합니다.");
    }
}
```

## 4. 구현 일정

### Day 1: Core Foundation
- [x] Entity 클래스 구현 (DailyRoutineEntity, DailyReflectionEntity, Enums)
- [x] Repository 인터페이스 구현
- [x] 기본 연속성 계산 로직 구현

### Day 2: Service Layer
- [x] DailyRoutineService 구현
- [x] DailyReflectionService 구현  
- [x] ConsecutiveDaysCalculator 완성
- [x] 단위 테스트 작성

### Day 3: Integration Layer
- [x] DailyRecordFacade 구현
- [x] DTO 클래스들 구현
- [x] 비즈니스 로직 통합

### Day 4: API Layer
- [x] DailyRecordController 구현
- [x] API 문서화 (Swagger)
- [x] 예외 처리 로직

### Day 5: Testing & Polishing
- [x] 통합 테스트 작성
- [x] 엣지 케이스 테스트
- [x] 성능 테스트 및 최적화
- [x] 코드 리뷰 및 리팩토링

## 5. 주요 기술적 고려사항

### 5.1 성능 최적화
- **복합 인덱스**: member_id + performed_date 조합으로 빠른 조회
- **배치 처리**: 루틴 기록을 벌크로 저장하여 DB 호출 최소화
- **지연 로딩**: 연관 엔티티는 LAZY 로딩으로 성능 향상
- **N+1 문제 해결**: `findAllByIds()` 메서드로 bulk 조회하여 DB 호출 최소화
- **BaseTimeEntity 활용**: 기존 아키텍처 일관성 유지로 타임스탬프 자동 관리

### 5.2 데이터 정합성
- **유니크 제약**: 동일 유저+루틴+날짜 중복 방지
- **트랜잭션 관리**: 루틴 기록과 회고를 원자적으로 처리
- **소프트 참조**: 루틴 삭제 시에도 기록 보존

### 5.3 확장성 고려
- **도메인 분리**: daily 패키지로 독립적 관리
- **추상화**: 향후 주간/월간 통계 기능 확장 용이
- **API 버전 관리**: RESTful API 설계로 하위 호환성 보장

## 6. API 명세

### 6.1 일일 기록 저장
```http
POST /api/daily-records/{date}
Content-Type: application/json
Authorization: Bearer {token}

{
  "reflection": {
    "content": "오늘은 좋은 하루였다",
    "emotion": "HAPPY"
  },
  "routineRecords": [
    {
      "routineId": 1,
      "performanceLevel": "FULL_SUCCESS"
    },
    {
      "routineId": 2,
      "performanceLevel": "PARTIAL_SUCCESS"
    }
  ]
}
```

### 6.2 일일 기록 조회
```http
GET /api/daily-records/{date}
Authorization: Bearer {token}
```

### 6.3 오늘 기록 조회 (편의 API)
```http
GET /api/daily-records/today
Authorization: Bearer {token}
```

### 6.4 응답 형식
```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    "reflection": {
      "content": "오늘은 좋은 하루였다",
      "emotion": "HAPPY",
      "reflectionDate": "2025-01-25"
    },
    "routineRecords": [
      {
        "routineId": 1,
        "routineTitle": "물 8잔 마시기",
        "category": "HEALTH",
        "performanceLevel": "FULL_SUCCESS",
        "consecutiveDays": 5,
        "isGrowthMode": true,
        "targetType": "COUNT",
        "targetValue": 8,
        "growthCycleDays": 7,
        "targetIncrement": 1
      }
    ],
    "allRoutines": [
      {
        "routineId": 1,
        "title": "물 8잔 마시기",
        "category": "HEALTH",
        "isGrowthMode": false
      }
    ]
  }
}
```

## 7. 추가 구현 고려사항

### 7.1 예외 처리
- **당일 외 수정 시도**: `IllegalArgumentException`
- **존재하지 않는 루틴**: `RoutineNotFoundException`
- **권한 없음**: `AccessDeniedException`

### 7.2 로깅 전략
- **수행 기록 변경**: INFO 레벨로 로깅
- **연속성 달성**: WARN 레벨로 특별 로깅 (알림용)
- **시스템 오류**: ERROR 레벨로 로깅

## 8. 성능 개선 사항 (Code Review 반영)

### 8.1 BaseTimeEntity 일관성 유지
- **기존 아키텍처 활용**: 프로젝트에서 이미 사용 중인 `BaseTimeEntity` 상속으로 `created_at`, `updated_at` 자동 관리
- **SQL DDL 단순화**: 타임스탬프 컬럼을 SQL에서 제거하고 JPA 어노테이션으로 관리

### 8.2 N+1 쿼리 문제 해결
- **문제점**: `enrichRoutineRecords()` 메서드에서 루틴 개수만큼 반복적인 `findById()` 호출
- **해결방안**: 
  1. 모든 `routineId`를 `Set`으로 수집
  2. `routineService.findAllByIds(routineIds)` 단일 쿼리로 bulk 조회
  3. `Map<Long, RoutineEntity>`로 변환하여 O(1) 조회
- **성능 향상**: N+1 쿼리 → 단일 쿼리로 DB 호출 최소화

### 8.3 추가 구현 필요사항
- **RoutineService에 추가 필요**: `findAllByIds(Collection<Long> ids)` 메서드 구현
- **예외 처리 강화**: 존재하지 않는 루틴 ID에 대한 `RoutineNotFoundException` 처리

### 8.4 향후 검토사항 (팀 논의 필요)
- **API 메서드 변경**: POST → PUT/PATCH 방식 도입 검토
  - PUT: 전체 리소스 대체 (RESTful 원칙 부합)
  - PATCH: 부분 업데이트 (유연성 제공)
- **비즈니스 요구사항**: 현재 UX 패턴과의 일치성 고려 필요

이 문서를 기반으로 단계별 구현을 진행하시면 됩니다!