# Daily 도메인 API 명세서

> **최신 업데이트**: 2025-01-04  
> **버전**: 1.5.0  
> **신규 기능**: 월별 통계 조회 API  
> **개선사항**: 일별 루틴 성공률 통계 제공  

---

## 📋 개요

Daily 도메인은 사용자의 **일일 루틴 수행 기록**과 **회고**를 관리하는 API를 제공합니다.

### 핵심 기능
- 특정 날짜의 루틴 수행 기록 저장 및 조회
- 일일 회고(감정 포함) 기록 관리  
- 연속 수행 일수 자동 계산
- 성장 주기 관리: currentCycleDays 자동 업데이트
- 실패 주기 관리: failureCycleDays 자동 업데이트
- 루틴 성장 모드 스냅샷 저장
- **🆕 월별 통계**: 일별 루틴 성공률 통계 조회

### 아키텍처 개선사항 (v1.2.0)
- **CQS 패턴**: Command/Query 책임 분리
- **Domain Service**: 복잡한 비즈니스 로직 분리  
- **Facade 경량화**: 단순 조율 역할로 변경

---

## 🔐 인증cl

**모든 API는 JWT 인증이 필요합니다.**

```http
Authorization: Bearer {JWT_TOKEN}
```

- **권한 요구사항**: `ROLE_USER`
- **JWT 토큰 획득**: `/api/auth/social/login` 또는 `/api/dev/auth/mock-login`

---

## 📍 Base URL

```
{base_url}/api/daily-records
```

---

## 🛠️ API Endpoints

### 1. 일일 기록 저장

특정 날짜의 루틴 수행 기록과 회고를 저장합니다.

```http
POST /api/daily-records/{date}
```

#### Parameters

| Parameter | Type | Required | Format | Description |
|-----------|------|----------|--------|-------------|
| `date` | Path | ✅ | `yyyy-MM-dd` | 기록할 날짜 (미래 날짜 불가) |

#### Request Body

```json
{
  "reflection": {
    "content": "string",
    "emotion": "HAPPY | SOSO | SAD | MAD"
  },
  "routineRecords": [
    {
      "routineId": "number",
      "performanceLevel": "FULL_SUCCESS | PARTIAL_SUCCESS | NOT_PERFORMED"
    }
  ]
}
```

#### Request Body Schema

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reflection` | Object | ❌ | 일일 회고 정보 (optional) |
| `reflection.content` | String | ❌ | 회고 내용 |
| `reflection.emotion` | Enum | ✅ | 감정 상태 |
| `routineRecords` | Array | ❌ | 루틴 수행 기록 목록 (optional) |
| `routineRecords[].routineId` | Long | ✅ | 루틴 ID (사용자 소유 루틴만) |
| `routineRecords[].performanceLevel` | Enum | ✅ | 수행 정도 |

#### Emotion Types

| Value | Description |
|-------|-------------|
| `HAPPY` | 행복 |
| `SOSO` | 그저그래 |
| `SAD` | 슬픔 |
| `MAD` | 화남 |

#### Performance Levels

| Value | Description |
|-------|-------------|
| `FULL_SUCCESS` | 완전성공 |
| `PARTIAL_SUCCESS` | 부분성공 |
| `NOT_PERFORMED` | 미수행 |

#### Response

```json
{
  "code": "S200",
  "message": "success",
  "data": {
    "reflection": {
      "content": "오늘은 루틴을 잘 실행한 좋은 하루였다!",
      "emotion": "HAPPY",
      "reflectionDate": "2025-01-31"
    },
    "routineRecords": [
      {
        "routineId": 1,
        "routineTitle": "매일 운동하기",
        "category": "HEALTH",
        "performanceLevel": "FULL_SUCCESS",
        "consecutiveDays": 5,
        "currentCycleDays": 2,
        "failureCycleDays": 0,
        "isGrowthMode": true,
        "targetType": "COUNT",
        "targetValue": 30,
        "growthCycleDays": 7,
        "targetIncrement": 5
      }
    ],
    "allRoutines": []
  }
}
```

#### 비즈니스 로직

1. **날짜 검증**: 미래 날짜는 저장 불가
2. **루틴 권한 검증**: 본인 소유 루틴만 기록 가능
3. **중복 처리**: 동일 날짜 기록 시 **업데이트** (덮어쓰기)
4. **연속 일수 계산**: `FULL_SUCCESS`인 경우만 연속 일수 증가
5. **🆕 성장/실패 주기 관리**: 
   - `FULL_SUCCESS`: currentCycleDays 증가 (+1), failureCycleDays 리셋 (0)
   - `PARTIAL_SUCCESS` / `NOT_PERFORMED`: failureCycleDays 증가 (+1), currentCycleDays 리셋 (0)
6. **스냅샷 저장**: 루틴의 성장 모드 설정값을 기록 시점에 저장

---

### 2. 특정 날짜 기록 조회

특정 날짜의 루틴 수행 기록과 회고를 조회합니다.

```http
GET /api/daily-records/{date}
```

#### Parameters

| Parameter | Type | Required | Format | Description |
|-----------|------|----------|--------|-------------|
| `date` | Path | ✅ | `yyyy-MM-dd` | 조회할 날짜 |

#### Response

```json
{
  "code": "S200",
  "message": "success",
  "data": {
    "reflection": {
      "content": "오늘은 루틴을 잘 실행한 좋은 하루였다!",
      "emotion": "HAPPY",
      "reflectionDate": "2025-01-31"
    },
    "routineRecords": [
      {
        "routineId": 1,
        "routineTitle": "매일 운동하기",
        "category": "HEALTH",
        "performanceLevel": "FULL_SUCCESS",
        "consecutiveDays": 5,
        "currentCycleDays": 2,
        "failureCycleDays": 0,
        "isGrowthMode": true,
        "targetType": "COUNT",
        "targetValue": 30,
        "growthCycleDays": 7,
        "targetIncrement": 5
      }
    ],
    "allRoutines": [
      {
        "routineId": 1,
        "category": "HEALTH",
        "title": "매일 운동하기",
        "isGrowthMode": true,
        "targetType": "COUNT",
        "targetValue": 30,
        "growthCycleDays": 7,
        "targetIncrement": 5,
        "currentCycleDays": 2,
        "failureCycleDays": 0,
        "createdAt": "2025-01-01T10:00:00"
      },
      {
        "routineId": 2,
        "category": "STUDY", 
        "title": "영어 공부",
        "isGrowthMode": false,
        "targetType": "TIME",
        "targetValue": 60,
        "createdAt": "2025-01-15T14:30:00"
      }
    ]
  }
}
```

#### Response Schema

| Field | Type | Description |
|-------|------|-------------|
| `reflection` | Object \| null | 해당 날짜의 회고 정보 |
| `routineRecords` | Array | 해당 날짜에 기록된 루틴 수행 목록 |
| `allRoutines` | Array | 사용자의 모든 활성 루틴 목록 |

---

### 3. 오늘 기록 조회

오늘 날짜의 루틴 수행 기록과 회고를 조회합니다. (편의 API)

```http
GET /api/daily-records/today
```

#### Response

특정 날짜 조회 API와 동일한 응답 구조이지만, `allRoutines` 배열이 포함됩니다.

---

### 4. 월별 통계 조회 🆕

특정 월에 대해 각 일별로 성공한 루틴(FullSuccess)/전체 루틴 수와 성공률을 조회합니다.

```http
GET /api/daily-records/monthly-stats/{year}/{month}
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `year` | Path | ✅ | 조회할 연도 (예: 2025) |
| `month` | Path | ✅ | 조회할 월 (1-12) |

#### Response

```json
{
  "code": "S215",
  "message": "월별 통계 조회 성공",
  "data": {
    "year": 2025,
    "month": 1,
    "dailyStats": [
      {
        "day": 1,
        "successfulRoutines": 3,
        "totalRoutines": 5,
        "successRate": 60.0
      },
      {
        "day": 2,
        "successfulRoutines": 0,
        "totalRoutines": 0,
        "successRate": 0.0
      },
      {
        "day": 3,
        "successfulRoutines": 4,
        "totalRoutines": 4,
        "successRate": 100.0
      }
    ]
  }
}
```

#### Response Schema

| Field | Type | Description |
|-------|------|-------------|
| `year` | Integer | 조회한 연도 |
| `month` | Integer | 조회한 월 |
| `dailyStats` | Array | 일별 통계 배열 |
| `dailyStats[].day` | Integer | 해당 월의 일 (1-31) |
| `dailyStats[].successfulRoutines` | Integer | 성공한 루틴 수 (FULL_SUCCESS만 카운트) |
| `dailyStats[].totalRoutines` | Integer | 전체 루틴 수 |
| `dailyStats[].successRate` | Double | 성공률 (%) - 소수점 둘째 자리까지 |

#### 비즈니스 로직

1. **성공 기준**: `FULL_SUCCESS`만 성공으로 카운트
2. **기록 없는 날**: 0/0/0%로 표시 
3. **전체 날짜 포함**: 해당 월의 1일부터 말일까지 모든 날짜 반환
4. **성공률 계산**: totalRoutines > 0일 때만 계산, 소수점 둘째 자리까지 반올림

---

## ❌ 에러 응답

### 공통 에러 형식

```json
{
  "code": "E400",
  "message": "Bad Request",
  "data": null
}
```

### 주요 에러 코드

| HTTP Status | Error Code | Description | 상황 |
|-------------|------------|-------------|------|
| `401` | `E401` | Unauthorized | JWT 토큰 없음/만료 |
| `400` | `E400` | Bad Request | 잘못된 요청 파라미터 |
| `400` | `DAILY_RECORD_FUTURE_DATE_NOT_ALLOWED` | 미래 날짜 수정 불가 | 미래 날짜로 기록 저장 시도 |
| `400` | `MEMBER_NOT_FOUND` | 사용자 없음 | 유효하지 않은 JWT |
| `400` | `ROUTINE_NOT_FOUND` | 루틴 없음 | 존재하지 않는 routineId |
| `400` | `ROUTINE_ACCESS_DENIED` | 루틴 접근 권한 없음 | 다른 사용자의 루틴 접근 |

### Validation 에러 예시

```json
{
  "code": "E400", 
  "message": "Validation failed",
  "data": {
    "invalidRoutines": [
      {
        "routineId": 9999,
        "errorType": "NOT_FOUND",
        "message": "루틴을 찾을 수 없습니다"
      },
      {
        "routineId": 123,
        "errorType": "ACCESS_DENIED", 
        "message": "해당 루틴에 접근할 권한이 없습니다"
      }
    ]
  }
}
```

---

## 📊 Response Data Schema

### DailyRecordResponse

| Field | Type | Description |
|-------|------|-------------|
| `reflection` | ReflectionResponse \| null | 회고 정보 |
| `routineRecords` | RoutineRecordResponse[] | 수행 기록 목록 |
| `allRoutines` | RoutineResponse[] | 전체 루틴 목록 |

### MonthlyStatsResponse 🆕

| Field | Type | Description |
|-------|------|-------------|
| `year` | Integer | 조회한 연도 |
| `month` | Integer | 조회한 월 |
| `dailyStats` | DailyStat[] | 일별 통계 목록 |

### DailyStat 🆕

| Field | Type | Description |
|-------|------|-------------|
| `day` | Integer | 해당 월의 일 (1-31) |
| `successfulRoutines` | Integer | 성공한 루틴 수 (FULL_SUCCESS만) |
| `totalRoutines` | Integer | 전체 루틴 수 |
| `successRate` | Double | 성공률 (%) |

### ReflectionResponse

| Field | Type | Description |
|-------|------|-------------|
| `content` | String | 회고 내용 |
| `emotion` | EmotionType | 감정 상태 |
| `reflectionDate` | LocalDate | 회고 날짜 |

### RoutineRecordResponse

| Field | Type | Description |
|-------|------|-------------|
| `routineId` | Long | 루틴 ID |
| `routineTitle` | String | 루틴 제목 |
| `category` | RoutineCategory | 루틴 카테고리 |
| `performanceLevel` | PerformanceLevel | 수행 정도 |
| `consecutiveDays` | Integer | 연속 수행 일수 (총 누적) |
| `currentCycleDays` | Integer | 🆕 현재 성공 주기 일수 |
| `failureCycleDays` | Integer | 🆕 현재 실패 주기 일수 |
| `isGrowthMode` | Boolean | 성장 모드 여부 |
| `targetType` | TargetType | 목표 타입 |
| `targetValue` | Integer | 목표값 |
| `growthCycleDays` | Integer | 성장 주기 |
| `targetIncrement` | Integer | 목표 증가량 |

### RoutineResponse (allRoutines)

| Field | Type | Description |
|-------|------|-------------|
| `routineId` | Long | 루틴 ID |
| `category` | RoutineCategory | 카테고리 |
| `title` | String | 제목 |
| `isGrowthMode` | Boolean | 성장 모드 여부 |
| `targetType` | TargetType | 목표 타입 |
| `targetValue` | Integer | 목표값 |
| `growthCycleDays` | Integer | 성장 주기 (성장 모드일 때) |
| `targetIncrement` | Integer | 목표 증가량 (성장 모드일 때) |
| `currentCycleDays` | Integer | 🆕 현재 성공 주기 일수 |
| `failureCycleDays` | Integer | 🆕 현재 실패 주기 일수 |
| `createdAt` | LocalDateTime | 생성일시 |

---

## 🔧 사용 예시

### Case 1: 완전한 일일 기록 저장

```http
POST /api/daily-records/2025-01-31
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

{
  "reflection": {
    "content": "오늘은 루틴을 모두 완수해서 뿌듯한 하루였다!",
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

### Case 2: 회고만 저장

```http
POST /api/daily-records/2025-01-30

{
  "reflection": {
    "content": "오늘은 바빠서 루틴을 못했지만, 내일은 꼭 하자!",
    "emotion": "SOSO"
  }
}
```

### Case 3: 루틴 기록만 저장

```http
POST /api/daily-records/2025-01-29

{
  "routineRecords": [
    {
      "routineId": 1,
      "performanceLevel": "NOT_PERFORMED"
    }
  ]
}
```

---

## 🔄 성장/실패 주기 관리 시스템 🆕

### 개요
일일 기록 저장 시 성장 모드 루틴의 `currentCycleDays`와 `failureCycleDays`를 자동으로 관리합니다.

### 동작 원리
1. **성공 기록 (FULL_SUCCESS)**: 
   - `currentCycleDays += 1`
   - `failureCycleDays = 0` (실패 카운터 리셋)
2. **실패 기록 (PARTIAL_SUCCESS, NOT_PERFORMED)**: 
   - `failureCycleDays += 1`
   - `currentCycleDays = 0` (성공 카운터 리셋)
3. **성장 조건**: `currentCycleDays >= growthCycleDays`
4. **감소 조건**: `failureCycleDays >= growthCycleDays`
5. **적응 후**: 해당 카운터가 0으로 리셋 (루틴 API에서 처리)

### 예시 시나리오

**푸쉬업 루틴 (목표: 10개, 성장주기: 3일)**

```
Day 1: FULL_SUCCESS 기록
  → consecutiveDays: 1, currentCycleDays: 1, failureCycleDays: 0

Day 2: FULL_SUCCESS 기록  
  → consecutiveDays: 2, currentCycleDays: 2, failureCycleDays: 0

Day 3: FULL_SUCCESS 기록
  → consecutiveDays: 3, currentCycleDays: 3, failureCycleDays: 0
  → 성장 조건 만족! (currentCycleDays >= 3)

사용자 선택:
- 목표 증가: 12개 목표, currentCycleDays → 0
- 주기 리셋: 10개 목표 유지, currentCycleDays → 0

Day 4: PARTIAL_SUCCESS 기록
  → consecutiveDays: 3 (유지), currentCycleDays: 0, failureCycleDays: 1

Day 5: NOT_PERFORMED 기록
  → consecutiveDays: 0 (리셋), currentCycleDays: 0, failureCycleDays: 2

Day 6: NOT_PERFORMED 기록
  → consecutiveDays: 0 (유지), currentCycleDays: 0, failureCycleDays: 3
  → 감소 조건 만족! (failureCycleDays >= 3)
```

### 필드 비교

| 필드 | 설명 | 증가 조건 | 리셋 조건 |
|------|------|----------|----------|
| `consecutiveDays` | 전체 연속 성공 일수 | FULL_SUCCESS 기록 시 | FULL_SUCCESS 외의 기록 시 |
| `currentCycleDays` | 현재 성공 주기 일수 | FULL_SUCCESS 기록 시 | 실패 기록 또는 성장/리셋 액션 시 |
| `failureCycleDays` | 현재 실패 주기 일수 | 실패 기록 시 | FULL_SUCCESS 기록 또는 감소/리셋 액션 시 |

### 관련 API
- **적응 확인**: `GET /api/routines/adaptation-check`
- **루틴 적응**: `PATCH /api/routines/{id}/target?action=INCREASE|DECREASE|RESET`

---

## 🚀 개선사항 (v1.2.0)

### 아키텍처 개선
- **CQS 패턴**: 읽기/쓰기 작업 분리로 성능 최적화 기반 마련
- **Domain Service**: 복잡한 비즈니스 로직을 별도 서비스로 분리
- **Facade 경량화**: 단순한 조율 역할로 책임 명확화

### 코드 품질 개선  
- **불필요한 메서드 제거**: 5개 메서드 + 7개 Import 정리
- **Import 최적화**: 각 서비스별 필요한 의존성만 유지
- **컴파일 안정성**: 모든 변경사항 컴파일 검증 완료

### 유지보수성 향상
- **단일 책임 원칙**: 각 클래스의 역할이 더욱 명확해짐
- **테스트 용이성**: 단위 테스트 작성이 쉬워짐
- **확장성**: 새로운 기능 추가 시 기존 코드 영향 최소화

---

## 📚 관련 문서

- [Daily 도메인 리팩토링 리뷰](./daily-improve.txt)
- [Postman 테스트 플랜](./DAILY_POSTMAN_TESTS.md)
- [Routine 도메인 API 명세서](./ROUTINE_API_SPECIFICATION.md)

---

## 📞 문의

API 관련 문의사항이 있으시면 개발팀에 문의해 주세요.

**최종 업데이트**: 2025-01-04