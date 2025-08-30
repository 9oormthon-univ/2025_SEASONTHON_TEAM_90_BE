# 🌟 Habiglow API 명세서

> **소셜 로그인 전용 Spring Boot JWT 인증 시스템 API 문서**

## 🔄 최근 업데이트 (v2.0)

### 🔒 보안 강화 업데이트
- **전체 사용자 조회 API 제거**: `GET /api/users` 보안상 완전 제거
- **개인정보 보호 강화**: `GET /api/users/{id}` → `GET /api/users/me` (본인만 조회)
- **계정 보안 강화**: `DELETE /api/users/{id}` → `DELETE /api/users/me` (본인만 삭제)
- **JWT 기반 본인 인증**: 토큰에서 사용자 ID 추출하여 본인 확인

### 🛠️ 시스템 개선
- **예외 처리 강화**: 포괄적 예외 핸들러 및 민감 정보 마스킹 추가
- **코드 품질 개선**: 중복 코드 제거 및 데드코드 정리
- **트랜잭션 최적화**: 읽기 전용 트랜잭션 기본 적용으로 성능 향상

---

## 📋 목차
- [📌 API 개요](#-api-개요)
- [🔐 인증 방식](#-인증-방식)
- [🏗️ 응답 구조](#-응답-구조)
- [📄 API 엔드포인트](#-api-엔드포인트)
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
- **소셜 로그인 전용**: 일반 회원가입/로그인 불가, OAuth2 소셜 로그인만 지원
- **JWT 기반 인증**: Access Token(1시간) + Refresh Token(24시간)
- **플랫폼별 사용자 분리**: socialUniqueId 기반 완전 분리 정책
- **통합 응답 구조**: 모든 API가 `CommonApiResponse<T>` 구조로 응답
- **개발용 Mock API**: dev 프로파일에서만 사용 가능한 테스트 API 제공

---

## 🔐 인증 방식

### JWT 토큰 명세
```yaml
Access Token:
  - 유효시간: 1시간
  - 저장위치: Authorization header
  - 형식: Bearer {token}
  - 용도: API 인증

Refresh Token:
  - 유효시간: 24시간  
  - 저장위치: HttpOnly Cookie
  - 용도: Access Token 갱신
  - 보안: XSS 공격 방지
```

### 인증 헤더
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

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
| POST | `/api/auth/social/login` | 클라이언트 소셜 로그인 | ❌ |
| POST | `/api/auth/token/refresh` | Access Token 재발급 | 🟡 Refresh Token |
| POST | `/api/auth/token/refresh/full` | 전체 토큰 재발급 | 🟡 Refresh Token |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |
| GET | `/api/users/me` | 내 정보 조회 | ✅ |
| GET | `/api/users/me/interests` | 내 관심사 조회 | ✅ |
| PUT | `/api/users/me/interests` | 관심사 수정 | ✅ |
| DELETE | `/api/users/me` | 내 계정 삭제 | ✅ |
| GET | `/api/routine-categories` | 루틴 카테고리 목록 조회 | ❌ |
| POST | `/api/dev/auth/register` | 개발용 Mock 회원가입 | ❌ (dev only) |
| POST | `/api/dev/auth/mock-login` | 개발용 Mock 로그인 | ❌ (dev only) |

## 1. 🔐 인증 관리 API

### 1.1 Access Token 재발급
Access Token만 새로 발급받습니다.

**요청**
```http
POST /api/auth/token/refresh
Cookie: refresh={refresh_token}
```

**응답**
```json
{
  "code": "S202",
  "message": "Access 토큰 재발급 성공",
  "data": {
    "accessToken": "Bearer eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": false
  }
}
```

### 1.2 Access + Refresh Token 모두 재발급
Access Token과 Refresh Token을 모두 새로 발급받습니다.

**요청**
```http
POST /api/auth/token/refresh/full
Cookie: refresh={refresh_token}
```

**응답**
```json
{
  "code": "S203", 
  "message": "Access/Refresh 토큰 재발급 성공",
  "data": {
    "accessToken": "Bearer eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer", 
    "expiresIn": 3600,
    "refreshTokenIncluded": true
  }
}
```

### 1.3 로그아웃
현재 토큰을 무효화하고 로그아웃합니다.

**요청**
```http
POST /api/auth/logout
Authorization: Bearer {access_token}
Cookie: refresh={refresh_token}
```

**응답**
```json
{
  "code": "S201",
  "message": "로그아웃 성공",
  "data": null
}
```

---

## 2. 👤 회원 관리 API

### 2.1 내 정보 조회
로그인한 사용자의 정보를 조회합니다.

**요청**
```http
GET /api/members/me
Authorization: Bearer {access_token}
```

**응답**
```json
{
  "code": "S208",
  "message": "회원 정보 조회 성공",
  "data": {
    "id": 1,
    "memberName": "홍길동", 
    "memberEmail": "hong@example.com",
    "socialType": "GOOGLE",
    "profileImageUrl": "https://lh3.googleusercontent.com/a/example",
    "interests": [
      {
        "code": "HEALTH",
        "description": "건강"
      },
      {
        "code": "LEARNING",
        "description": "학습"
      }
    ]
  }
}
```

**에러 응답**
```json
{
  "code": "E401",
  "message": "인증되지 않은 사용자",
  "data": null
}
```

### 2.2 내 계정 삭제
로그인한 사용자의 계정을 삭제합니다.

**요청**
```http
DELETE /api/members/me
Authorization: Bearer {access_token}
```

**응답**
```json
{
  "code": "S207",
  "message": "회원 삭제 성공",
  "data": null
}
```

**에러 응답**
```json
{
  "code": "E401",
  "message": "인증되지 않은 사용자",
  "data": null
}
```

### 2.3 내 관심사 조회
로그인한 사용자의 관심사 목록을 조회합니다.

**요청**
```http
GET /api/members/me/interests
Authorization: Bearer {access_token}
```

**응답**
```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    "memberId": 1,
    "interests": [
      {
        "code": "HEALTH",
        "description": "건강"
      },
      {
        "code": "LEARNING", 
        "description": "학습"
      }
    ]
  }
}
```

### 2.4 내 관심사 수정
로그인한 사용자의 관심사를 수정합니다.

**요청**
```http
PUT /api/members/me/interests
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "interests": ["HEALTH", "DIET", "LEARNING"]
}
```

**응답**
```json
{
  "code": "S200",
  "message": "성공", 
  "data": null
}
```

---

## 3. 🏷️ 루틴 카테고리 API

### 3.1 루틴 카테고리 목록 조회
회원이 선택할 수 있는 모든 루틴 카테고리 목록을 조회합니다.

**요청**
```http
GET /api/routine-categories
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
    }
  ]
}
```

---

## 4. 🛠️ 개발용 인증 API (dev 프로파일 전용)

### 3.1 개발용 Mock 회원가입
테스트용 사용자를 생성합니다.

**요청**
```http
POST /api/dev/auth/register
Content-Type: application/json

{
  "email": "test@example.com",
  "name": "테스트유저",
  "socialType": "KAKAO", 
  "mockSocialId": "mock_user_001"
}
```

**응답**
```json
{
  "code": "S205",
  "message": "회원가입 성공",
  "data": null
}
```

### 3.2 개발용 Mock 로그인
기존 테스트용 사용자로 로그인하여 JWT 토큰을 발급받습니다.

**요청**
```http
POST /api/dev/auth/mock-login
Content-Type: application/json

{
  "email": "test@example.com",
  "name": "테스트유저", 
  "socialType": "KAKAO",
  "mockSocialId": "mock_user_001"
}
```

**응답**
```json
{
  "code": "S209",
  "message": "소셜 로그인 성공",
  "data": {
    "accessToken": "Bearer eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": true
  }
}
```

---

## 4. 🌐 클라이언트 기반 소셜 로그인

### 클라이언트 소셜 로그인
클라이언트에서 소셜 로그인을 처리한 후 서버에서 JWT 토큰을 발급받습니다.

**요청**
```http
POST /api/auth/social/login
Content-Type: application/json

{
  "socialAccessToken": "클라이언트에서 받은 소셜 액세스 토큰",
  "socialType": "GOOGLE" // GOOGLE, KAKAO, NAVER
}
```

**응답**
```json
{
  "code": "S209",
  "message": "소셜 로그인 성공",
  "data": {
    "accessToken": "Bearer eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenIncluded": true
  }
}
```

### 지원 소셜 플랫폼
| 플랫폼 | 소셜 타입 | 설명 |
|--------|----------|---------|
| 🟢 Google | `GOOGLE` | 구글 소셜 로그인 |
| 🟡 Kakao | `KAKAO` | 카카오 소셜 로그인 |
| 🟦 Naver | `NAVER` | 네이버 소셜 로그인 |

### 클라이언트 기반 소셜 로그인 플로우
1. **클라이언트**: 소셜 제공업체에서 OAuth2 로그인 처리
2. **클라이언트**: 소셜 액세스 토큰 획득
3. **클라이언트**: 서버의 `/api/auth/social/login`에 소셜 토큰 전송
4. **서버**: 소셜 토큰을 제공업체 API로 검증
5. **서버**: 사용자 정보 추출 및 회원 생성/조회
6. **서버**: JWT 토큰 발급 및 Refresh Token 쿠키 설정
7. **클라이언트**: JWT 토큰으로 API 요청

---

## ⚠️ 에러 코드

### 4xx 클라이언트 에러
| 코드 | 메시지 | HTTP Status | 설명 |
|------|--------|-------------|------|
| E400 | 잘못된 형식의 토큰입니다 | 400 | TOKEN_MALFORMED |
| E400 | 잘못된 입력값입니다 | 400 | INVALID_INPUT_VALUE |
| E400 | 파라미터 검증에 실패했습니다 | 400 | PARAMETER_VALIDATION_ERROR |
| E401 | 이메일 또는 비밀번호가 틀렸습니다 | 401 | LOGIN_FAIL |
| E401 | 유효하지 않은 토큰입니다 | 401 | INVALID_TOKEN |
| E401 | 만료된 토큰입니다 | 401 | TOKEN_EXPIRED |
| E401 | 리프레시 토큰을 찾을 수 없습니다 | 401 | REFRESH_TOKEN_NOT_FOUND |
| E401 | 액세스 토큰이 필요합니다 | 401 | ACCESS_TOKEN_REQUIRED |
| E401 | 차단된 토큰입니다 | 401 | TOKEN_BLACKLISTED |
| E401 | 소셜 로그인에 실패했습니다 | 401 | OAUTH2_LOGIN_FAILED |
| E404 | 회원을 찾을 수 없습니다 | 404 | MEMBER_NOT_FOUND |
| E409 | 이미 가입된 이메일입니다 | 409 | DUPLICATE_EMAIL |
| E429 | 너무 많은 요청입니다 | 429 | TOO_MANY_REQUESTS |

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
      "key": "email",
      "value": "invalid-email",
      "reason": "올바른 이메일 형식이 아닙니다."
    },
    {
      "key": "name", 
      "value": "null",
      "reason": "사용자 이름은 필수입니다."
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
1. **기본 인증 플로우**: Mock 로그인 → 내 정보 조회 → 로그아웃
2. **토큰 갱신 플로우**: 로그인 → Access Token 갱신 → 전체 토큰 갱신
3. **회원 관리 플로우**: 내 정보 조회 → 내 계정 삭제
4. **보안 테스트**: 토큰 없이 접근 시도 → 401 에러 확인
5. **에러 케이스**: 잘못된 토큰, 만료된 토큰, 블랙리스트 토큰

---

## 🔐 소셜 타입
- **GOOGLE**: 구글 소셜 로그인
- **NAVER**: 네이버 소셜 로그인
- **KAKAO**: 카카오 소셜 로그인

---

## 📝 참고사항

### 🔒 보안 강화 사항
1. **개인정보 보호**: 사용자는 오직 본인의 정보만 조회/삭제할 수 있습니다.
2. **전체 사용자 조회 제거**: 보안상의 이유로 전체 사용자 목록 조회 API는 제거되었습니다.
3. **토큰 기반 인증**: JWT 토큰에서 사용자 ID를 추출하여 본인 확인을 수행합니다.
4. **민감 정보 마스킹**: 로그에서 password, token, secret 등의 민감 정보는 자동으로 마스킹됩니다.

### 🛡️ 시스템 보안
1. **소셜 로그인 전용**: 일반 회원가입/로그인은 지원하지 않습니다.
2. **토큰 보안**: Refresh Token은 HttpOnly Cookie로 관리되어 XSS 공격을 방지합니다.
3. **토큰 블랙리스트**: 로그아웃 시 Access Token이 블랙리스트에 등록되어 재사용을 방지합니다.
4. **Rate Limiting**: OAuth2 로그인 엔드포인트는 5회/분 제한이 적용됩니다.

### 🔧 기술적 사항
1. **개발용 API**: `/api/dev/` 경로의 API는 dev, local 프로파일에서만 사용 가능합니다.
2. **사용자 분리**: 플랫폼별 사용자는 socialUniqueId로 완전 분리 관리됩니다.
3. **트랜잭션 최적화**: 읽기 전용 트랜잭션을 기본으로 사용하여 성능을 최적화했습니다.
4. **예외 처리 강화**: 포괄적인 예외 처리로 안정적인 에러 응답을 제공합니다.

---

**이 API 명세서는 Habiglow 소셜 로그인 전용 JWT 인증 시스템의 모든 엔드포인트와 사용법을 포함하고 있습니다.***