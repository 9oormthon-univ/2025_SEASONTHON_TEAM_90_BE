# 🌟 Habiglow API 명세서

> **소셜 로그인 전용 Spring Boot JWT 인증 시스템 API 문서**

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

## 2. 👥 소셜 회원 관리 API

### 2.1 전체 사용자 목록 조회
가입된 모든 사용자의 정보를 조회합니다.

**요청**
```http
GET /api/users
Authorization: Bearer {access_token}
```

**응답**
```json
{
  "code": "S208",
  "message": "회원 정보 조회 성공", 
  "data": [
    {
      "id": 1,
      "memberName": "홍길동",
      "memberEmail": "hong@example.com",
      "socialType": "GOOGLE"
    },
    {
      "id": 2, 
      "memberName": "김철수",
      "memberEmail": "kim@example.com",
      "socialType": "KAKAO"
    }
  ]
}
```

### 2.2 특정 사용자 조회
사용자 ID를 기준으로 회원 정보를 조회합니다.

**요청**
```http
GET /api/users/{id}
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
    "socialType": "GOOGLE"
  }
}
```

### 2.3 사용자 삭제
지정한 사용자 ID에 해당하는 회원을 삭제합니다.

**요청**
```http
DELETE /api/users/{id}
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

---

## 3. 🛠️ 개발용 인증 API (dev 프로파일 전용)

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
1. **기본 인증 플로우**: Mock 로그인 → 사용자 목록 조회 → 로그아웃
2. **토큰 갱신 플로우**: 로그인 → Access Token 갱신 → 전체 토큰 갱신
3. **회원 관리 플로우**: 전체 조회 → 특정 조회 → 삭제
4. **에러 케이스**: 잘못된 토큰, 존재하지 않는 사용자, 만료된 토큰

---

## 🔐 소셜 타입
- **GOOGLE**: 구글 소셜 로그인
- **NAVER**: 네이버 소셜 로그인
- **KAKAO**: 카카오 소셜 로그인

---

## 📝 참고사항

1. **소셜 로그인 전용**: 일반 회원가입/로그인은 지원하지 않습니다.
2. **개발용 API**: `/api/dev/` 경로의 API는 dev, local 프로파일에서만 사용 가능합니다.
3. **토큰 보안**: Refresh Token은 HttpOnly Cookie로 관리되어 XSS 공격을 방지합니다.
4. **Rate Limiting**: OAuth2 로그인 엔드포인트는 5회/분 제한이 적용됩니다.
5. **사용자 분리**: 플랫폼별 사용자는 socialUniqueId로 완전 분리 관리됩니다.

---

**이 API 명세서는 Habiglow 소셜 로그인 전용 JWT 인증 시스템의 모든 엔드포인트와 사용법을 포함하고 있습니다.**