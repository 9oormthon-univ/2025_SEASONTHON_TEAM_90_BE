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

## 💡 성장 모드 설명

### 🚀 점진적 과부하 원리
성장 모드는 **점진적 과부하(Progressive Overload)** 원리를 적용한 핵심 기능입니다.

### 동작 방식
1. **초기 목표 설정**: 달성 가능한 목표로 시작
2. **성장 주기 설정**: 목표를 증가시킬 주기 (예: 7일마다)
3. **증가량 설정**: 주기마다 증가할 수치 (예: +50)
4. **자동 증가**: 설정된 주기에 따라 목표 자동 증가

### 예시 시나리오
```
초기 목표: 100개 푸시업 (7일 주기, +10개씩 증가)
1주차: 100개
2주차: 110개  
3주차: 120개
...
```

### 설정 필드
- `isGrowthMode`: 성장 모드 활성화 여부
- `targetType`: 목표 타입 (`NUMBER` 또는 `DATE`)
- `targetValue`: 현재 목표 수치
- `growthCycleDays`: 성장 주기 (일 단위)
- `targetIncrement`: 주기당 증가량

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