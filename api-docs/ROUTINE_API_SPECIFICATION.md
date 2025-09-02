# 🏋️ Habiglow 루틴 API 명세서

> **점진적 과부하와 성장 중심의 루틴 관리 시스템**

---

## 📋 목차
- [📌 API 개요](#-api-개요)
- [🏗️ 데이터 모델](#-데이터-모델)
- [📄 API 엔드포인트](#-api-엔드포인트)
- [💡 성장 모드 설명](#-성장-모드-설명)
- [⚠️ 검증 규칙](#-검증-규칙)

---

## 📌 API 개요

### 기본 정보
- **Base URL**: `/api/routines`
- **인증**: 모든 API는 JWT 토큰 필요
- **권한**: 사용자는 자신의 루틴만 접근 가능

### 핵심 기능
- **루틴 CRUD**: 생성, 조회, 수정, 삭제
- **성장 모드**: 점진적 목표 증가 시스템
- **카테고리별 분류**: 9개 카테고리로 루틴 구분
- **목표 타입**: 숫자형/날짜형 목표 설정

---

## 🏗️데이터 모델

### 루틴 카테고리
| 코드 | 한글명 | 설명 |
|------|--------|------|
| `HABIT_IMPROVEMENT` | 습관 개선 | 나쁜 습관 고치기 |
| `HEALTH` | 건강 | 운동, 체력 관리 |
| `LEARNING` | 학습 | 공부, 스킬 향상 |
| `MINDFULNESS` | 마음 챙김 | 명상, 감정 관리 |
| `EXPENSE_MANAGEMENT` | 소비 관리 | 가계부, 절약 |
| `HOBBY` | 취미 | 개인 관심사 |
| `DIET` | 식습관 | 식단 관리 |
| `SLEEP` | 수면 | 수면 패턴 관리 |
| `SELF_CARE` | 자기관리 | 개인 케어 |

### 목표 타입
| 타입 | 설명 | 예시 |
|------|------|------|
| `NUMBER` | 숫자 | 500회, 30분, 5km |
| `DATE` | 날짜 | 2024-12-31까지 |

### 루틴 응답 구조
```json
{
  "routineId": 1,
  "category": "HEALTH",
  "title": "매일 운동하기",
  "description": "건강한 생활을 위해 매일 30분씩 운동하기",
  "isGrowthMode": true,
  "targetType": "NUMBER",
  "targetValue": 500,
  "growthCycleDays": 7,
  "targetIncrement": 50,
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:00"
}
```

---

## 📄 API 엔드포인트

## 1. 루틴 생성

### 📝 새 루틴 생성
**POST** `/api/routines`

**Request Body:**
```json
{
  "category": "HEALTH",                    // 필수: 루틴 카테고리
  "title": "매일 운동하기",                  // 필수: 루틴 제목 (100자 이하)
  "description": "건강한 생활을 위해 매일 30분씩 운동하기",  // 선택: 설명 (500자 이하)
  "isGrowthMode": true,                   // 선택: 성장 모드 활성화 (기본값: false)
  "targetType": "NUMBER",                 // 성장 모드 시 필수
  "targetValue": 500,                     // 성장 모드 시 필수
  "growthCycleDays": 7,                   // 성장 모드 시 필수: 성장 주기(일)
  "targetIncrement": 50                   // 성장 모드 시 필수: 증가량
}
```

**Response:**
```json
{
  "code": "S210",
  "message": "루틴이 성공적으로 생성되었습니다",
  "data": {
    "routineId": 1,
    "category": "HEALTH",
    "title": "매일 운동하기",
    "description": "건강한 생활을 위해 매일 30분씩 운동하기",
    "isGrowthMode": true,
    "targetType": "NUMBER",
    "targetValue": 500,
    "growthCycleDays": 7,
    "targetIncrement": 50,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 2. 루틴 조회

### 📋 내 루틴 전체 목록
**GET** `/api/routines`

**Response:**
```json
{
  "code": "S214",
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
        "targetValue": 500,
        "growthCycleDays": 7,
        "targetIncrement": 50,
        "createdAt": "2025-01-01T00:00:00",
        "updatedAt": "2025-01-01T00:00:00"
      }
    ],
    "totalCount": 1
  }
}
```

### 🏷️ 카테고리별 루틴 목록
**GET** `/api/routines/category?category={카테고리명}`

**Query Parameters:**
- `category` (required): 조회할 카테고리 (`HEALTH`, `LEARNING` 등)

**Example:** `/api/routines/category?category=HEALTH`

**Response:** 동일한 구조의 목록 응답

---

### 🔍 루틴 상세 조회
**GET** `/api/routines/{routineId}`

**Path Parameters:**
- `routineId`: 조회할 루틴 ID

**Response:**
```json
{
  "code": "S211",
  "message": "루틴 조회 성공",
  "data": {
    "routineId": 1,
    "category": "HEALTH",
    "title": "매일 운동하기",
    "description": "건강한 생활을 위해 매일 30분씩 운동하기",
    "isGrowthMode": true,
    "targetType": "NUMBER",
    "targetValue": 500,
    "growthCycleDays": 7,
    "targetIncrement": 50,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

---

## 3. 루틴 수정

### ✏️ 루틴 정보 수정
**PUT** `/api/routines/{routineId}`

**Path Parameters:**
- `routineId`: 수정할 루틴 ID

**Request Body:**
```json
{
  "category": "LEARNING",                  // 필수: 루틴 카테고리
  "description": "매일 1시간씩 새로운 기술 공부하기",  // 선택: 설명 수정
  "isGrowthMode": false,                  // 성장 모드 해제
  "targetType": null,                     // 성장 모드 해제 시 null
  "targetValue": null,
  "growthCycleDays": null,
  "targetIncrement": null
}
```

**참고:** 제목(`title`)은 수정할 수 없습니다.

**Response:**
```json
{
  "code": "S212",
  "message": "루틴이 성공적으로 수정되었습니다",
  "data": {
    /* 수정된 루틴 정보 */
  }
}
```

---

## 4. 루틴 삭제

### 🗑️ 루틴 삭제
**DELETE** `/api/routines/{routineId}`

**Path Parameters:**
- `routineId`: 삭제할 루틴 ID

**Response:**
```json
{
  "code": "S213",
  "message": "루틴이 성공적으로 삭제되었습니다",
  "data": null
}
```

---

## 5. 성장 루틴 관리 🚀

### 📈 성장 가능한 루틴 조회
**GET** `/api/routines/growth-check`

로그인 시 전날 기준으로 성장 주기를 완료한 루틴들을 조회합니다.

**Response:**
```json
{
  "code": "S200",
  "message": "요청이 성공적으로 처리되었습니다",
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
        "completedCycleDays": 3,
        "consecutiveDays": 3,
        "currentCycleDays": 3,
        "lastPerformedDate": "2025-01-30"
      }
    ],
    "totalGrowthReadyCount": 1
  }
}
```

**필드 설명:**
- `growthReadyRoutines`: 성장 가능한 루틴 목록
- `currentTarget`: 현재 목표값
- `nextTarget`: 성장 시 변경될 목표값
- `increment`: 증가량
- `completedCycleDays`: 설정된 성장 주기 일수
- `consecutiveDays`: 총 연속 성공 일수
- `currentCycleDays`: 현재 주기 내 연속 성공 일수
- `totalGrowthReadyCount`: 성장 가능한 루틴 총 개수

---

### 📊 루틴 목표치 증가
**PATCH** `/api/routines/{routineId}/increase-target`

성장 주기가 완료된 루틴의 목표치를 증가시킵니다.

**Path Parameters:**
- `routineId`: 목표를 증가시킬 루틴 ID

**성공 조건:**
- 성장 모드가 활성화된 루틴
- 성장 주기가 완료된 상태 (currentCycleDays >= growthCycleDays)
- 전날 FULL_SUCCESS 기록이 존재
- 목표 증가가 가능한 상태

**Response:**
```json
{
  "code": "S200",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": {
    "routineId": 1,
    "title": "푸쉬업 챌린지",
    "previousTarget": 10,
    "newTarget": 12,
    "increment": 2,
    "targetType": "NUMBER",
    "message": "목표가 10개에서 12개로 증가되었습니다!"
  }
}
```

**에러 응답:**
```json
{
  "code": "ROUTINE007",
  "message": "성장 모드가 활성화되지 않은 루틴입니다",
  "data": null
}
```

```json
{
  "code": "ROUTINE008", 
  "message": "아직 성장 주기가 완료되지 않았습니다",
  "data": null
}
```

---

### 🔄 성장 주기 리셋
**PATCH** `/api/routines/{routineId}/reset-growth-cycle`

성장 주기가 완료된 루틴의 주기를 리셋합니다. 성장을 거부할 때 사용합니다.

**Path Parameters:**
- `routineId`: 성장 주기를 리셋할 루틴 ID

**사용 시나리오:**
- 사용자가 목표 증가를 원하지 않는 경우
- 현재 목표를 더 연습하고 싶은 경우
- 성장 주기를 새로 시작하고 싶은 경우

**Response:**
```json
{
  "code": "S200",
  "message": "요청이 성공적으로 처리되었습니다", 
  "data": {
    "routineId": 1,
    "title": "푸쉬업 챌린지",
    "currentTarget": 10,
    "targetType": "NUMBER",
    "growthCycleDays": 3,
    "currentCycleDays": 0,
    "message": "성장 주기가 리셋되었습니다. 10개 목표로 새로운 3일 주기를 시작하세요!"
  }
}
```

**주요 변경사항:**
- `currentCycleDays`: 0으로 초기화
- 목표값은 그대로 유지
- 새로운 성장 주기 시작

---

## 💡 성장 모드 설명

### 🚀 점진적 과부하 원리
성장 모드는 **점진적 과부하(Progressive Overload)** 원리를 적용한 핵심 기능입니다.

### 동작 방식
1. **초기 목표 설정**: 달성 가능한 목표로 시작
2. **성장 주기 설정**: 목표를 증가시킬 주기 (예: 7일마다)
3. **증가량 설정**: 주기마다 증가할 수치 (예: +10개)
4. **자동 성장 감지**: 성장 주기 완료 시 사용자에게 알림
5. **사용자 선택**: 목표 증가 또는 주기 리셋 선택 가능

### 🔄 성장 주기 관리 시스템
- **consecutiveDays**: 총 연속 성공 일수 (누적)
- **currentCycleDays**: 현재 주기 내 연속 성공 일수
- **성장 조건**: `currentCycleDays >= growthCycleDays`
- **주기 리셋**: 목표 증가 시 `currentCycleDays = 0`으로 초기화

### 예시 시나리오
```
푸쉬업 루틴: 초기 10개 (3일 주기, +2개씩 증가)

1일차: 10개 성공 → currentCycleDays: 1, consecutiveDays: 1
2일차: 10개 성공 → currentCycleDays: 2, consecutiveDays: 2  
3일차: 10개 성공 → currentCycleDays: 3, consecutiveDays: 3
      ↓ 성장 조건 만족!
사용자 선택:
- 목표 증가 → 12개 목표, currentCycleDays: 0
- 주기 리셋 → 10개 목표 유지, currentCycleDays: 0
```

### 설정 필드
- `isGrowthMode`: 성장 모드 활성화 여부
- `targetType`: 목표 타입 (`NUMBER` 또는 `DATE`)
- `targetValue`: 현재 목표 수치
- `growthCycleDays`: 성장 주기 (일 단위)
- `targetIncrement`: 주기당 증가량
- `currentCycleDays`: 현재 주기 내 연속 성공 일수 🆕

---

## ⚠️ 검증 규칙

### 필수 입력값
| 필드 | 규칙 | 예외 상황 |
|------|------|-----------|
| `category` | 필수 | 없음 |
| `title` | 필수, 100자 이하 | 수정 시 제외 |

### 성장 모드 활성화 시 필수값
| 필드 | 규칙 |
|------|------|
| `targetType` | `NUMBER` 또는 `DATE` |
| `targetValue` | 양수 |
| `growthCycleDays` | 1 이상 |
| `targetIncrement` | 양수 |

### 선택 입력값
| 필드 | 규칙 |
|------|------|
| `description` | 500자 이하 |

### 에러 응답 예시
```json
{
  "code": "E400",
  "message": "입력값이 올바르지 않습니다",
  "data": [
    {
      "key": "title",
      "value": "",
      "reason": "루틴 제목은 필수입니다"
    },
    {
      "key": "targetValue",
      "value": "-10",
      "reason": "목표 수치는 양수여야 합니다"
    }
  ]
}
```

---

## 🎯 사용 예시

### 성장형 운동 루틴 생성
```json
{
  "category": "HEALTH",
  "title": "점진적 푸시업 도전",
  "description": "매주 10개씩 늘려가며 푸시업 실력 향상",
  "isGrowthMode": true,
  "targetType": "NUMBER",
  "targetValue": 50,
  "growthCycleDays": 7,
  "targetIncrement": 10
}
```

### 단순 습관 루틴 생성
```json
{
  "category": "MINDFULNESS",
  "title": "매일 명상하기",
  "description": "하루 10분 명상으로 마음 챙기기",
  "isGrowthMode": false
}
```

---

**🏋️ Habiglow 루틴 시스템으로 점진적 성장을 경험하세요!**