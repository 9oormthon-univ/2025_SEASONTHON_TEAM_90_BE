# 🏋️ Habiglow 루틴 API 명세서

> **점진적 과부하와 성장 중심의 루틴 관리 시스템 API 문서**

## 🔄 최근 업데이트 (v3.1)

### 🆕 신규 기능 (v3.1 - 2025-09-04)
- **실패 주기 카운터 추가**: `failureCycleDays` 필드로 감소 로직 개선
- **카운터 정보 응답 확대**: 모든 루틴 관련 API에 `currentCycleDays`, `failureCycleDays` 포함
- **적응 후 즉시 제외**: 성장/감소 액션 후 해당 루틴이 즉시 적응 대상에서 제거
- **완전한 리셋 기능**: RESET 액션 시 성공/실패 카운터 모두 0으로 초기화

### 🔧 아키텍처 개선 (v3.1 - 2025-09-04)
- **Facade 패턴 완전 적용**: Command(CUD)와 Query(R) Facade 분리로 CQRS 패턴 구현
- **성장 모드 서비스 분리**: `RoutineGrowthService`로 성장 로직 전문화
- **통합 적응형 API**: 성장/감소 대상 루틴을 한번에 확인할 수 있는 `/adaptation-check` 추가
- **액션 기반 목표 조정**: INCREASE, DECREASE, RESET 액션으로 통합된 `/target` 조정 API

### ⭐ 새로운 기능 (v2.5)
- **적응형 루틴 관리**: 성장 주기 완료 시 목표 증가 및 주기 리셋 기능
- **카테고리별 조회**: `/category` 엔드포인트로 특정 카테고리 루틴만 필터링 조회
- **성장 모드 스냅샷**: 일일 기록 저장 시 현재 성장 설정값 보존

### 🛠️ 시스템 개선
- **JWT 기반 인증 강화**: 모든 API에 `@PreAuthorize` 적용
- **Swagger 문서 자동화**: 자세한 API 문서 및 에러 응답 명세
- **유효성 검증 강화**: Bean Validation을 통한 입력값 검증

---

## 📋 목차
- [📌 API 개요](#-api-개요)
- [🔐 인증 방식](#-인증-방식)
- [🏗️ 응답 구조](#-응답-구조)
- [📄 API 엔드포인트](#-api-엔드포인트)
  - [1. 🔧 루틴 CRUD](#1--루틴-crud)
  - [2. 🏷️ 루틴 카테고리](#2--루틴-카테고리)
  - [3. 🚀 적응형 루틴 관리](#3--적응형-루틴-관리)
- [⚠️ 에러 코드](#-에러-코드)
- [🧪 테스트 가이드](#-테스트-가이드)

---

## 📌 API 개요

### 기본 정보
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **Authorization**: `Bearer {access_token}`
- **Swagger UI**: `http://localhost:8080/api-docs`

### 주요 특징
- **성장 모드 루틴 관리**: 점진적 과부하 원리를 적용한 목표 증가 시스템
- **JWT 기반 인증**: 모든 API가 사용자 인증 필요
- **카테고리별 관리**: HEALTH, LEARNING 등 다양한 루틴 분류
- **통합 응답 구조**: 모든 API가 `CommonApiResponse<T>` 구조로 응답
- **적응형 루틴**: 사용자 수행도에 따른 자동 목표 조정 제안

---

## 🔐 인증 방식

### JWT 토큰 인증
모든 루틴 관리 API는 JWT 토큰을 통한 사용자 인증이 필요합니다.

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 토큰 획득 방법
- **소셜 로그인**: `POST /api/auth/social/login`
- **개발용 로그인**: `POST /api/dev/auth/mock-login` (dev 프로파일)

---

## 🏗️ 응답 구조

### 공통 응답 형식
모든 API는 `CommonApiResponse<T>` 구조로 응답합니다.

```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    // 실제 응답 데이터
  }
}
```

### 응답 필드
- **code**: 응답 코드 (성공: S###, 실패: E###)
- **message**: 사용자 친화적 메시지
- **data**: 실제 응답 데이터 (성공시), 에러 상세정보 (실패시)

---

## 📄 API 엔드포인트

### API 엔드포인트 요약
| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| POST | `/api/routines` | 새 루틴 생성 | ✅ |
| GET | `/api/routines` | 내 루틴 목록 조회 | ✅ |
| GET | `/api/routines/category` | 카테고리별 내 루틴 목록 조회 | ✅ |
| GET | `/api/routines/{id}` | 루틴 상세 조회 | ✅ |
| PUT | `/api/routines/{id}` | 루틴 수정 | ✅ |
| DELETE | `/api/routines/{id}` | 루틴 삭제 | ✅ |
| GET | `/api/routines/categories` | 루틴 카테고리 목록 조회 | ❌ |
| GET | `/api/routines/adaptation-check` | 적응형 루틴 조정 대상 조회 | ✅ |
| PATCH | `/api/routines/{id}/target` | 루틴 목표 조정 | ✅ |

## 1. 🔧 루틴 CRUD

### 1.1 새 루틴 생성
새로운 루틴을 생성합니다.

**요청**
```http
POST /api/routines
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "category": "HEALTH",
  "title": "매일 운동하기",
  "description": "건강한 생활을 위해 매일 30분씩 운동하기",
  "isGrowthMode": true,
  "targetType": "NUMBER",
  "targetValue": 10,
  "growthCycleDays": 7,
  "targetIncrement": 2
}
```

**요청 필드**
  | 필드 | 타입 | 필수 여부 | 설명 |
  |---|---|---|---|
  | `category` | `String` | **Yes** | 루틴 카테고리 (`HEALTH`, `LEARNING` 등) |
  | `title` | `String` | **Yes** | 루틴 제목 (1~100자) |
  | `description` | `String` | No | 루틴 상세 설명 (최대 500자) |
  | `isGrowthMode` | `Boolean`| No | 성장 모드 활성화 여부 (기본값: `false`) |
  | `targetType` | `String` | 성장 모드 시 **Yes** | 목표 타입 (`NUMBER` 또는 `DATE`) |
  | `targetValue` | `Integer`| 성장 모드 시 **Yes** | 목표 수치 (양수) |
  | `growthCycleDays`| `Integer`| 성장 모드 시 **Yes** | 성장 주기(일) (1 이상) |
  | `targetIncrement`| `Integer`| 성장 모드 시 **Yes** | 주기당 목표 증가량 (양수) |

**응답**
```json
{
  "code": "S201",
  "message": "루틴 생성 성공",
  "data": {
    "routineId": 1,
    "category": "HEALTH",
    "title": "매일 운동하기",
    "description": "건강한 생활을 위해 매일 30분씩 운동하기",
    "isGrowthMode": true,
    "targetType": "NUMBER",
    "targetValue": 10,
    "growthCycleDays": 7,
    "targetIncrement": 2,
    "currentCycleDays": 0,
    "failureCycleDays": 0,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

**응답 필드**
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `routineId` | `Long` | 루틴의 고유 ID |
  | `category` | `String` | 루틴 카테고리 |
  | `title` | `String` | 루틴 제목 |
  | `description` | `String` | 루틴 상세 설명 |
  | `isGrowthMode`| `Boolean`| 성장 모드 활성화 여부 |
  | `targetType` | `String` | 목표 타입 (`NUMBER` 또는 `DATE`) |
  | `targetValue` | `Integer`| 현재 목표 수치 |
  | `growthCycleDays`| `Integer`| 설정된 성장 주기 (일) |
  | `targetIncrement`| `Integer`| 주기당 목표 증가량 |
  | `currentCycleDays` | `Integer` | 현재 성공 주기 일수 (0부터 시작) |
  | `failureCycleDays` | `Integer` | 현재 실패 주기 일수 (0부터 시작) |
  | `createdAt` | `String` | 생성 일시 (ISO 8601 형식) |
  | `updatedAt` | `String` | 마지막 수정 일시 (ISO 8601 형식) |

---

### 1.2 내 루틴 목록 조회
현재 로그인한 사용자의 모든 루틴을 조회합니다.

**요청**
```http
GET /api/routines
Authorization: Bearer {access_token}
```

**응답**
```json
{
  "code": "S200",
  "message": "루틴 목록 조회 성공",
  "data": {
    "routines": [
      {
        "routineId": 1,
        "category": "HEALTH",
        "title": "매일 운동하기",
        "description": "건강한 생활을 위해 매일 30분씩 운동하기",
        "isGrowthMode": true,
        "targetType": "NUMBER",
        "targetValue": 10,
        "growthCycleDays": 7,
        "targetIncrement": 2,
        "currentCycleDays": 3,
        "failureCycleDays": 0,
        "createdAt": "2025-01-01T00:00:00",
        "updatedAt": "2025-01-01T00:00:00"
      }
    ],
    "totalCount": 1
  }
}
```
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `routines` | `Array` | `RoutineResponse` 객체의 배열 |
  | `totalCount` | `Integer` | 조회된 루틴의 총 개수 |

---

### 1.3 카테고리별 내 루틴 목록 조회
현재 로그인한 사용자의 특정 카테고리 루틴을 조회합니다.

**요청**
```http
GET /api/routines/category?category=HEALTH
Authorization: Bearer {access_token}
```

**요청 파라미터**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `category` | String | ✅ | 조회할 루틴 카테고리 (예: `HEALTH`) |

**응답**
위 `내 루틴 목록 조회`와 동일한 형식입니다.

---

### 1.4 루틴 상세 조회
특정 루틴의 상세 정보를 조회합니다.

**요청**
```http
GET /api/routines/{routineId}
Authorization: Bearer {access_token}
```

**경로 파라미터**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `routineId` | Long | ✅ | 조회할 루틴의 고유 ID |

**응답**
위 `새 루틴 생성`의 응답 형식과 동일합니다.

**에러 응답**
```json
{
  "code": "E404",
  "message": "루틴을 찾을 수 없습니다",
  "data": null
}
```

---

### 1.5 루틴 정보 수정
기존 루틴의 정보를 수정합니다. **(제목은 수정 불가)**

**요청**
```http
PUT /api/routines/{routineId}
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "category": "LEARNING",
  "description": "수정된 설명",
  "isGrowthMode": false,
  "targetType": "TIME",
  "targetValue": 30
}
```

**주요 차이점**
- `title` 필드는 수정할 수 없습니다
- 성장 모드를 비활성화하면 관련 필드들은 무시됩니다

**응답**
위 `새 루틴 생성`의 응답 형식과 동일합니다.

---

### 1.6 루틴 삭제
특정 루틴을 삭제합니다.

**요청**
```http
DELETE /api/routines/{routineId}
Authorization: Bearer {access_token}
```

**경로 파라미터**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `routineId` | Long | ✅ | 삭제할 루틴의 고유 ID |

**응답**
```json
{
  "code": "S204",
  "message": "루틴 삭제 성공",
  "data": null
}
```

---

## 2. 🏷️ 루틴 카테고리

### 2.1 루틴 카테고리 목록 조회
선택 가능한 모든 루틴 카테고리 목록을 조회합니다.

**요청**
```http
GET /api/routines/categories
```

**응답**
```json
{
  "code": "S200",
  "message": "성공",
  "data": [
    {
      "code": "HEALTH",
      "description": "건강"
    },
    {
      "code": "LEARNING",
      "description": "학습"
    },
    {
      "code": "MINDFULNESS",
      "description": "마음 챙김"
    },
    {
      "code": "DIET",
      "description": "식습관"
    },
    {
      "code": "HOBBY",
      "description": "취미"
    },
    {
      "code": "SOCIAL",
      "description": "사회적 관계"
    },
    {
      "code": "WORK",
      "description": "업무"
    },
    {
      "code": "FINANCE",
      "description": "재정 관리"
    },
    {
      "code": "ENVIRONMENT",
      "description": "환경 정리"
    },
    {
      "code": "HABIT_IMPROVEMENT",
      "description": "습관 개선"
    }
  ]
}
```
**응답 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| `code` | String | 카테고리 영문 코드 (Enum 이름) |
| `description` | String | 카테고리 한글 표시 이름 |

---

## 3. 🚀 적응형 루틴 관리

### 3.1 적응형 루틴 조정 대상 조회
성장(목표 증가) 또는 감소(목표 하향) 대상이 되는 루틴 목록을 통합 조회합니다.

**요청**
```http
GET /api/routines/adaptation-check
Authorization: Bearer {access_token}
```

**응답**
```json
{
  "code": "S200",
  "message": "적응형 루틴 조정 대상 조회 성공",
  "data": {
    "growthReadyRoutines": [
      {
        "routineId": 1,
        "title": "푸쉬업 챌린지",
        "category": "HEALTH",
        "targetType": "NUMBER",
        "currentTarget": 10,
        "nextTarget": 12,
        "increment": 2,
        "completedCycleDays": 7,
        "consecutiveDays": 7,
        "currentCycleDays": 7,
        "failureCycleDays": 0,
        "lastPerformedDate": "2025-09-03"
      }
    ],
    "reductionReadyRoutines": [
      {
        "routineId": 2,
        "title": "독서하기",
        "currentTargetValue": 60,
        "suggestedTargetValue": 50,
        "currentCycleDays": 2,
        "failureCycleDays": 7,
        "lastAttemptDate": "2025-09-01"
      }
    ],
    "totalGrowthReadyCount": 1,
    "totalReductionReadyCount": 1,
    "totalAdaptiveCount": 2
  }
}
```
**응답 필드**
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `growthReadyRoutines` | `Array` | 목표 **증가** 후보 루틴 목록 |
  | `reductionReadyRoutines` | `Array` | 목표 **감소** 후보 루틴 목록 |
  | `totalGrowthReadyCount` | `Integer` | 성장 후보 루틴 총 개수 |
  | `totalReductionReadyCount` | `Integer` | 감소 후보 루틴 총 개수 |
  | `totalAdaptiveCount` | `Integer` | 전체 조정 대상 루틴 개수 |

  **성장 후보 루틴(`GrowthReadyRoutineResponse`) 필드**:
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `routineId` | `Long` | 루틴 ID |
  | `title` | `String` | 루틴 제목 |
  | `category` | `String` | 루틴 카테고리 |
  | `targetType` | `String` | 목표 타입 |
  | `currentTarget` | `Integer` | 현재 목표치 |
  | `nextTarget` | `Integer` | 다음 목표치 |
  | `increment` | `Integer` | 증가량 |
  | `completedCycleDays` | `Integer` | 완료된 성장 주기(일) |
  | `consecutiveDays` | `Integer` | 연속 성공일 |
  | `currentCycleDays` | `Integer` | 현재 주기 연속일 |
  | `failureCycleDays` | `Integer` | 현재 실패 주기 일수 |
  | `lastPerformedDate` | `String` | 마지막 수행 날짜 |

  **감소 후보 루틴(`ReductionReadyRoutineResponse`) 필드**:
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `routineId` | `Long` | 루틴 ID |
  | `title` | `String` | 루틴 제목 |
  | `currentTargetValue` | `Integer` | 현재 목표치 |
  | `suggestedTargetValue` | `Integer` | 제안 목표치 |
  | `currentCycleDays` | `Integer` | 현재 주기 연속일 |
  | `failureCycleDays` | `Integer` | 실패 주기 일수 |
  | `lastAttemptDate` | `String` | 마지막 시도 날짜 |

---

### 3.2 루틴 목표 조정
루틴의 목표를 증가, 감소 또는 주기를 리셋합니다.

**요청**
```http
PATCH /api/routines/{routineId}/target?action=INCREASE
Authorization: Bearer {access_token}
```

**경로 파라미터**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `routineId` | Long | ✅ | 조정할 루틴의 고유 ID |

**쿼리 파라미터**
| Parameter | Type | Required | Description | Options |
|-----------|------|----------|-------------|----------|
| `action` | String | ✅ | 조정 액션 | `INCREASE`, `DECREASE`, `RESET` |

**응답**
```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    "routineId": 1,
    "routineTitle": "푸쉬업 챌린지",
    "previousValue": 10,
    "newValue": 12,
    "action": "INCREASE"
  }
}
```

**액션 타입별 동작**
- **INCREASE**: 목표값을 targetIncrement만큼 증가시키고 currentCycleDays를 0으로 리셋
- **DECREASE**: 목표값을 targetIncrement만큼 감소시키고 failureCycleDays를 0으로 리셋  
- **RESET**: 목표값은 유지하고 currentCycleDays와 failureCycleDays를 모두 0으로 리셋

**에러 응답**
```json
{
  "code": "E400",
  "message": "조정 조건을 만족하지 않습니다",
  "data": {
    "reason": "성장 주기가 완료되지 않았습니다"
  }
}
```

## ⚠️ 에러 코드

### 4xx 클라이언트 에러
| 코드 | 메시지 | HTTP Status | 설명 |
|------|--------|-------------|------|
| E400 | 잘못된 입력값입니다 | 400 | INVALID_INPUT_VALUE |
| E400 | 파라미터 검증에 실패했습니다 | 400 | PARAMETER_VALIDATION_ERROR |
| E401 | 인증되지 않은 사용자입니다 | 401 | ACCESS_TOKEN_REQUIRED |
| E401 | 유효하지 않은 토큰입니다 | 401 | INVALID_TOKEN |
| E401 | 만료된 토큰입니다 | 401 | TOKEN_EXPIRED |
| E403 | 해당 루틴에 접근할 권한이 없습니다 | 403 | ROUTINE_ACCESS_DENIED |
| E404 | 루틴을 찾을 수 없습니다 | 404 | ROUTINE_NOT_FOUND |
| E404 | 회원을 찾을 수 없습니다 | 404 | MEMBER_NOT_FOUND |
| E409 | 이미 존재하는 루틴 제목입니다 | 409 | DUPLICATE_ROUTINE_TITLE |

### 5xx 서버 에러
| 코드 | 메시지 | HTTP Status | 설명 |
|------|--------|-------------|------|
| E500 | 내부 서버 오류가 발생했습니다 | 500 | INTERNAL_SERVER_ERROR |

### Bean Validation 에러 응답 예시
```json
{
  "code": "E400",
  "message": "파라미터 검증에 실패했습니다",
  "data": [
    {
      "key": "title",
      "value": "",
      "reason": "루틴 제목은 필수입니다."
    },
    {
      "key": "targetValue",
      "value": "-1",
      "reason": "목표값은 양수여야 합니다."
    }
  ]
}
```

---

## 🧪 테스트 가이드

### Postman 환경 변수 설정
```json
{
  "base_url": "http://localhost:8080",
  "access_token": ""
}
```

### 자동 토큰 발급 Pre-request Script
```javascript
// Collection 레벨에 설정
const mockLoginRequest = {
    url: pm.environment.get("base_url") + "/api/dev/auth/mock-login",
    method: 'POST',
    header: { 'Content-Type': 'application/json' },
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            email: "test@example.com",
            name: "테스트유저",
            socialType: "KAKAO",
            mockSocialId: "mock_user_001"
        })
    }
};

pm.sendRequest(mockLoginRequest, function (err, response) {
    if (!err && response.code === 200) {
        const responseData = response.json();
        pm.environment.set("access_token", responseData.data.accessToken);
    }
});
```

### 테스트 시나리오
1. **기본 CRUD 플로우**: 루틴 생성 → 목록 조회 → 상세 조회 → 수정 → 삭제
2. **성장 모드 플로우**: 성장 모드 루틴 생성 → 일일 기록 완료 → 적응 대상 확인 → 목표 증가
3. **카테고리 관리**: 카테고리 목록 조회 → 카테고리별 루틴 필터링
4. **적응형 루틴**: 성장/감소 후보 확인 → 목표 조정 (INCREASE/DECREASE/RESET)
5. **에러 케이스**: 권한 없는 루틴 접근, 존재하지 않는 루틴, 유효성 검증 실패

---

## 📝 참고사항

### 🔒 보안 및 권한
1. **JWT 인증 필수**: 모든 루틴 관리 API는 유효한 JWT 토큰 필요
2. **사용자별 격리**: 사용자는 본인의 루틴만 접근 가능
3. **권한 검증**: `@PreAuthorize` 어노테이션으로 메서드 레벨 보안 적용

### 🚀 성장 모드 시스템
1. **점진적 과부하**: 일정 주기마다 목표를 단계적으로 증가
2. **적응형 조정**: 사용자 수행도에 따른 목표 증가/감소 제안
3. **이중 주기 관리**: 
   - `currentCycleDays`: 현재 성공 주기 진행도 추적
   - `failureCycleDays`: 현재 실패 주기 진행도 추적
4. **스냅샷 보존**: 일일 기록 시점의 성장 설정값 보존
5. **자동 리셋**: 성장/감소 액션 후 해당 카운터 자동 초기화

### 🛠️ 기술적 세부사항
1. **Facade 패턴**: Command(CUD)와 Query(R) 책임 분리
2. **Bean Validation**: 입력값 검증을 위한 표준 어노테이션 사용
3. **Swagger 통합**: 자동화된 API 문서 생성
4. **예외 처리**: 전역 예외 핸들러로 일관된 에러 응답

---

**이 API 명세서는 Habiglow 루틴 관리 시스템의 모든 엔드포인트와 사용법을 포함하고 있습니다.**
