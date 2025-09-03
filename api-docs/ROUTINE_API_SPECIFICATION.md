# 🏋️ Habiglow 루틴 API 명세서 (v2.2)

> **점진적 과부하와 성장 중심의 루틴 관리 시스템**

---

## 📄 API 엔드포인트

## 1. 루틴 CRUD

### 📝 새 루틴 생성
**POST** `/api/routines`

- **설명**: 새로운 루틴을 생성합니다.

- **요청 형식 (`CreateRoutineRequest`)**:
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

- **응답 형식 (`RoutineResponse`)**:
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
  | `createdAt` | `String` | 생성 일시 (ISO 8601 형식) |
  | `updatedAt` | `String` | 마지막 수정 일시 (ISO 8601 형식) |

---

### 📋 내 루틴 목록 조회
**GET** `/api/routines`

- **설명**: 현재 로그인한 사용자의 모든 루틴을 조회합니다.
- **응답 형식 (`RoutineListResponse`)**:
  ```json
  {
    "routines": [
      {
        "routineId": 1,
        "category": "HEALTH",
        "title": "매일 운동하기",
        "description": "...",
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
  ```
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `routines` | `Array` | `RoutineResponse` 객체의 배열 |
  | `totalCount` | `Integer` | 조회된 루틴의 총 개수 |

---

### 🏷️ 카테고리별 내 루틴 목록 조회
**GET** `/api/routines/category`

- **설명**: 현재 로그인한 사용자의 특정 카테고리 루틴을 조회합니다.
- **요청 형식 (Query Parameters)**:
  - `category` (required, `String`): 조회할 루틴 카테고리 (예: `HEALTH`)
- **응답 형식 (`RoutineListResponse`)**: 위 `내 루틴 목록 조회`와 동일합니다.

---

### 🔍 루틴 상세 조회
**GET** `/api/routines/{routineId}`

- **설명**: 특정 루틴의 상세 정보를 조회합니다.
- **요청 형식 (Path Parameters)**:
  - `routineId` (required, `Long`): 조회할 루틴의 고유 ID
- **응답 형식 (`RoutineResponse`)**: 위 `새 루틴 생성`의 응답 형식과 동일합니다.

---

### ✏️ 루틴 정보 수정
**PUT** `/api/routines/{routineId}`

- **설명**: 기존 루틴의 정보를 수정합니다. **(제목은 수정 불가)**
- **요청 형식 (`UpdateRoutineRequest`)**: `새 루틴 생성`의 요청 형식과 유사하나 `title` 필드가 없습니다.
- **응답 형식 (`RoutineResponse`)**: 위 `새 루틴 생성`의 응답 형식과 동일합니다.

---

### 🗑️ 루틴 삭제
**DELETE** `/api/routines/{routineId}`

- **설명**: 특정 루틴을 삭제합니다.
- **요청 형식 (Path Parameters)**:
  - `routineId` (required, `Long`): 삭제할 루틴의 고유 ID
- **응답 형식**: `200 OK` (Content 없음)

---

## 2. 루틴 카테고리

### 📚 루틴 카테고리 목록 조회
**GET** `/api/routines/categories`

- **설명**: 선택 가능한 모든 루틴 카테고리 목록을 조회합니다.
- **응답 형식 (`List<RoutineCategoryResponse>`)**:
  ```json
  [
    {
      "code": "HABIT_IMPROVEMENT",
      "displayName": "습관 개선"
    },
    {
      "code": "HEALTH",
      "displayName": "건강"
    }
  ]
  ```
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `code` | `String` | 카테고리 영문 코드 (Enum 이름) |
  | `displayName` | `String` | 카테고리 한글 표시 이름 |

---

## 3. 적응형 루틴 관리 🚀

### 📈 적응 대상 루틴 조회
**GET** `/api/routines/adaptation-check`

- **설명**: 성장(목표 증가) 또는 감소(목표 하향) 대상이 되는 루틴 목록을 조회합니다.
- **응답 형식 (`AdaptiveRoutineCheckResponse`)**:
  ```json
  {
    "growthCandidates": { /* GrowthCandidate 객체 */ },
    "reductionCandidates": { /* ReductionCandidate 객체 */ }
  }
  ```
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `growthCandidates` | `Object` | 목표 **증가** 후보 루틴 정보 |
  | `reductionCandidates` | `Object` | 목표 **감소** 후보 루틴 정보 |

  **`GrowthCandidate` 상세**:
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `candidates` | `Array` | 성장 후보 루틴 객체 배열 |
  | `totalCount` | `Integer` | 성장 후보 루틴 총 개수 |
  | `type` | `String` | `GROWTH` 고정값 |

  **`ReductionCandidate` 상세**:
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `candidates` | `Array` | 감소 후보 루틴 객체 배열 |
  | `totalCount` | `Integer` | 감소 후보 루틴 총 개수 |
  | `type` | `String` | `REDUCTION` 고정값 |

---

### 🔧 루틴 목표 조정
**PATCH** `/api/routines/{routineId}/target`

- **설명**: 루틴의 목표를 조정(증가, 감소, 주기 리셋)합니다.
- **요청 형식 (Query Parameters)**:
  - `action` (required, `String`): 조정 액션 (`INCREASE`, `DECREASE`, `RESET`)
- **응답 형식 (`RoutineAdaptationResultResponse`)**:
  ```json
  {
    "routineId": 1,
    "routineTitle": "푸쉬업 챌린지",
    "previousValue": 10,
    "newValue": 12,
    "action": "INCREASE"
  }
  ```
  | 필드 | 타입 | 설명 |
  |---|---|---|
  | `routineId` | `Long` | 조정된 루틴의 고유 ID |
  | `routineTitle` | `String` | 조정된 루틴의 제목 |
  | `previousValue`| `Integer`| 변경 전 수치 (목표값 또는 주기 일수) |
  | `newValue` | `Integer`| 변경 후 수치 (목표값 또는 주기 일수) |
  | `action` | `String` | 수행된 액션 (`INCREASE`, `DECREASE`, `RESET`) |
