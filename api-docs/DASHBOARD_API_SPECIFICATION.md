# 📊 Habiglow 대시보드 API 명세서

> **주간 대시보드 및 AI 인사이트 시스템 API 문서**

## 🔄 최근 업데이트 (v1.0)

### 🆕 신규 기능 (v1.0 - 2025-01-04)
- **주간 대시보드 집계**: 상단 KPI, 감정 분포, 요일별 완료율, 네비게이션 정보 제공
- **주간 AI 인사이트**: 특정 주차 인사이트 조회/생성 기능
- **지난 주 인사이트**: 가장 최근 지난 주 인사이트 자동 조회/생성

### 🛠️ 시스템 특징
- **JWT 기반 인증**: 모든 API가 사용자 인증 필요
- **통합 응답 구조**: 모든 API가 `CommonApiResponse<T>` 구조로 응답
- **캐싱 지원**: 동일 입력 시 기존 인사이트 재사용
- **강제 재생성**: force 파라미터로 기존 저장 무시하고 재생성

---

## 📋 목차
- [📌 API 개요](#-api-개요)
- [🔐 인증 방식](#-인증-방식)
- [🏗️ 응답 구조](#-응답-구조)
- [📄 API 엔드포인트](#-api-엔드포인트)
  - [1. 📊 주간 대시보드](#1--주간-대시보드)
  - [2. 🤖 주간 AI 인사이트](#2--주간-ai-인사이트)
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
- **주간 단위 분석**: 월요일부터 일요일까지 주간 단위 데이터 제공
- **JWT 기반 인증**: 모든 API가 사용자 인증 필요
- **AI 인사이트**: 사용자 루틴 수행 데이터 기반 개인화된 인사이트 제공
- **통합 응답 구조**: 모든 API가 `CommonApiResponse<T>` 구조로 응답
- **스마트 캐싱**: 동일 조건 재요청 시 기존 결과 재사용

---

## 🔐 인증 방식

### JWT 토큰 인증
모든 대시보드 API는 JWT 토큰을 통한 사용자 인증이 필요합니다.

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
| GET | `/v1/dashboard/weekly/stats` | 주간 대시보드 집계 조회 | ✅ |
| GET | `/v1/dashboard/weekly/insight` | 특정 주차 인사이트 조회/생성 | ✅ |
| GET | `/v1/dashboard/weekly/insight/last-week` | 지난 주 인사이트 조회/생성 | ✅ |

## 1. 📊 주간 대시보드

### 1.1 주간 대시보드 집계 조회
주간 KPI, 감정 분포, 요일별 완료율, 네비게이션 정보를 조회합니다.

**요청**
```http
GET /v1/dashboard/weekly/stats?memberId={memberId}&weekStart={weekStart}
Authorization: Bearer {access_token}
```

**쿼리 파라미터**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `memberId` | Long | ✅ | 사용자 ID (임시, 추후 @AuthUser로 대체 예정) | 1 |
| `weekStart` | Date | ✅ | 주 시작일(월요일 기준), ISO 8601 형식 | 2025-08-25 |

**응답**
```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    "period": {
      "weekStart": "2025-08-25",
      "weekEnd": "2025-08-31",
      "label": "8월 4째 주",
      "currentWeek": false,
      "complete": true,
      "nav": {
        "hasPrev": true,
        "hasNext": true,
        "prevWeekStart": "2025-08-18",
        "nextWeekStart": "2025-09-01"
      }
    },
    "metrics": {
      "totalRoutines": 6,
      "overall": {
        "done": 21,
        "total": 42,
        "rate": 50.0
      },
      "categories": [
        {
          "code": "HEALTH",
          "label": "건강",
          "done": 8,
          "total": 12,
          "rate": 66.7
        }
      ]
    },
    "emotionDistribution": {
      "HAPPY": 3,
      "SOSO": 2,
      "SAD": 1,
      "MAD": 0
    },
    "dailyCompletion": [
      {
        "date": "2025-08-25",
        "done": 3,
        "total": 6,
        "rate": 50.0,
        "mood": "HAPPY",
        "future": false
      }
    ]
  }
}
```

**응답 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `period` | Object | 주간 기간 정보 |
| `period.weekStart` | String | 주 시작일 (월요일) |
| `period.weekEnd` | String | 주 종료일 (일요일) |
| `period.label` | String | 주간 라벨 (예: "8월 4째 주") |
| `period.currentWeek` | Boolean | 현재 주 여부 |
| `period.complete` | Boolean | 주간 완료 여부 |
| `period.nav` | Object | 네비게이션 정보 |
| `period.nav.hasPrev` | Boolean | 이전 주 존재 여부 |
| `period.nav.hasNext` | Boolean | 다음 주 존재 여부 |
| `period.nav.prevWeekStart` | String | 이전 주 시작일 |
| `period.nav.nextWeekStart` | String | 다음 주 시작일 |
| `metrics` | Object | 루틴 수행 통계 |
| `metrics.totalRoutines` | Integer | 전체 루틴 수 |
| `metrics.overall` | Object | 전체 루틴 수행률 |
| `metrics.overall.done` | Integer | 완료한 루틴 수 |
| `metrics.overall.total` | Integer | 전체 루틴 수행 횟수 |
| `metrics.overall.rate` | Double | 전체 성공률 (%) |
| `metrics.categories` | Array | 카테고리별 수행 통계 |
| `metrics.categories[].code` | String | 카테고리 코드 |
| `metrics.categories[].label` | String | 카테고리 한글명 |
| `metrics.categories[].done` | Integer | 해당 카테고리 완료 수 |
| `metrics.categories[].total` | Integer | 해당 카테고리 전체 수 |
| `metrics.categories[].rate` | Double | 해당 카테고리 성공률 (%) |
| `emotionDistribution` | Object | 감정 분포 통계 |
| `emotionDistribution.HAPPY` | Integer | 행복 감정 기록 수 |
| `emotionDistribution.SOSO` | Integer | 보통 감정 기록 수 |
| `emotionDistribution.SAD` | Integer | 슬픔 감정 기록 수 |
| `emotionDistribution.MAD` | Integer | 화남 감정 기록 수 |
| `dailyCompletion` | Array | 일별 완료율 통계 |
| `dailyCompletion[].date` | String | 날짜 |
| `dailyCompletion[].done` | Integer | 완료한 루틴 수 |
| `dailyCompletion[].total` | Integer | 전체 루틴 수 |
| `dailyCompletion[].rate` | Double | 완료율 (%) |
| `dailyCompletion[].mood` | String | 해당 날짜 기분 |
| `dailyCompletion[].future` | Boolean | 미래 날짜 여부 |

---

## 2. 🤖 주간 AI 인사이트

### 2.1 특정 주차 인사이트 조회/생성
주차의 시작일(월요일)을 기준으로 인사이트를 1회 생성 후 저장하고, 동일 입력이면 재사용합니다.

**요청**
```http
GET /v1/dashboard/weekly/insight?memberId={memberId}&weekStart={weekStart}&force={force}
Authorization: Bearer {access_token}
```

**쿼리 파라미터**
| Parameter | Type | Required | Description | Default | Example |
|-----------|------|----------|-------------|---------|---------|
| `memberId` | Long | ✅ | 대상 사용자 ID | - | 1 |
| `weekStart` | Date | ✅ | 주 시작일(월요일), ISO 8601 형식 | - | 2025-08-25 |
| `force` | Boolean | ❌ | 기존 저장이 있어도 재생성 여부 | false | false |

**응답**
```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    "weekStart": "2025-08-25",
    "weekEnd": "2025-08-31",
    "insight": "이번 주는 건강 관련 루틴에서 66.7%의 높은 성취율을 보여주셨네요! 특히 월요일과 화요일에 집중력이 높았던 것 같습니다. 다음 주에는 수요일과 목요일 루틴 실행에 더 신경 써보시면 어떨까요?",
    "createdAt": "2025-09-04T10:30:00"
  }
}
```

**응답 필드 설명**
| 필드 | 타입 | 설명 |
|------|------|------|
| `weekStart` | String | 인사이트 대상 주 시작일 |
| `weekEnd` | String | 인사이트 대상 주 종료일 |
| `insight` | String | AI 생성 인사이트 내용 |
| `createdAt` | String | 인사이트 생성 일시 |

---

### 2.2 지난 주 인사이트 조회/생성
가장 최근 지난 주(월~일)의 인사이트를 조회/생성합니다.

**요청**
```http
GET /v1/dashboard/weekly/insight/last-week?memberId={memberId}&force={force}
Authorization: Bearer {access_token}
```

**쿼리 파라미터**
| Parameter | Type | Required | Description | Default | Example |
|-----------|------|----------|-------------|---------|---------|
| `memberId` | Long | ✅ | 대상 사용자 ID | - | 1 |
| `force` | Boolean | ❌ | 기존 저장이 있어도 재생성 여부 | false | false |

**응답**
위 `특정 주차 인사이트 조회/생성`과 동일한 형식입니다.

---

## ⚠️ 에러 코드

### 4xx 클라이언트 에러
| 코드 | 메시지 | HTTP Status | 설명 |
|------|--------|-------------|------|
| E400 | 잘못된 입력값입니다 | 400 | INVALID_INPUT_VALUE |
| E400 | 파라미터 검증에 실패했습니다 | 400 | PARAMETER_VALIDATION_ERROR |
| E401 | 인증되지 않은 사용자입니다 | 401 | ACCESS_TOKEN_REQUIRED |
| E401 | 유효하지 않은 토큰입니다 | 401 | INVALID_TOKEN |
| E401 | 만료된 토큰입니다 | 401 | TOKEN_EXPIRED |
| E403 | 해당 데이터에 접근할 권한이 없습니다 | 403 | ACCESS_DENIED |
| E404 | 회원을 찾을 수 없습니다 | 404 | MEMBER_NOT_FOUND |

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
      "key": "memberId",
      "value": "null",
      "reason": "사용자 ID는 필수입니다."
    },
    {
      "key": "weekStart",
      "value": "invalid-date",
      "reason": "올바른 날짜 형식이 아닙니다."
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
  "access_token": "",
  "test_member_id": "1"
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
1. **기본 대시보드 플로우**: 주간 대시보드 통계 조회 → 네비게이션으로 다른 주 조회
2. **AI 인사이트 플로우**: 특정 주차 인사이트 조회 → 지난 주 인사이트 조회
3. **캐싱 테스트**: 동일 조건으로 재요청 → force=true로 재생성
4. **에러 케이스**: 잘못된 날짜 형식, 존재하지 않는 사용자, 인증 실패

---

## 📝 참고사항

### 🔒 보안 및 권한
1. **JWT 인증 필수**: 모든 대시보드 API는 유효한 JWT 토큰 필요
2. **사용자별 격리**: 사용자는 본인의 데이터만 접근 가능
3. **권한 검증**: 각 API마다 사용자 권한 검증 수행

### 🤖 AI 인사이트 시스템
1. **스마트 캐싱**: 동일 조건 재요청 시 기존 결과 재사용으로 성능 최적화
2. **강제 재생성**: force 파라미터로 최신 데이터 반영한 새 인사이트 생성
3. **개인화**: 사용자별 루틴 수행 패턴 분석한 맞춤형 인사이트

### 🛠️ 기술적 세부사항
1. **주간 단위**: 월요일 시작, 일요일 종료하는 주간 단위 데이터 처리
2. **날짜 형식**: ISO 8601 형식 (yyyy-MM-dd) 사용
3. **응답 최적화**: 필요한 데이터만 포함하여 네트워크 효율성 향상
4. **예외 처리**: 전역 예외 핸들러로 일관된 에러 응답 제공

---

**이 API 명세서는 Habiglow 대시보드 시스템의 모든 엔드포인트와 사용법을 포함하고 있습니다.**