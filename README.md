# 🌟 Habiglow

> **해커톤 팀 프로젝트 - 점진적 과부하와 공감 중점의 루틴 서비스**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)

**📖 API 문서**: [API_SPECIFICATION.md](./API_SPECIFICATION.md) | **⚙️ 개발 문서**: [CLAUDE.md](./CLAUDE.md)

---

## 📋 **목차**

- [🎯 프로젝트 개요](#-프로젝트-개요)
- [⚡ 빠른 실행](#-빠른-실행)
- [🏗️ 기술 스택 & 아키텍처](#-기술-스택--아키텍처)
- [🔐 인증 시스템](#-인증-시스템)
- [📁 프로젝트 구조](#-프로젝트-구조)
- [🧪 개발 & 테스트](#-개발--테스트)
- [📚 문서 & 참고자료](#-문서--참고자료)

---

## 🎯 **프로젝트 개요**

**Habiglow**는 해커톤 팀 프로젝트로 개발된 **점진적 과부하와 공감 중점의 루틴 서비스**입니다.

### **핵심 특징**
- 🏋️ **점진적 과부하 루틴** - 사용자 맞춤형 루틴 계획 생성 및 관리
- 🤝 **공감 중점 소셜 기능** - 사용자 간 격려와 동기부여 시스템
- 🛡️ **안전한 소셜 인증** - OAuth2 기반 소셜 로그인 시스템

### **지원 소셜 플랫폼**
| 플랫폼 | 식별자 형식 | 상태 |
|--------|-------------|------|
| 🟢 Google | `GOOGLE_{id}` | ✅ 지원 |
| 🟡 Kakao | `KAKAO_{id}` | ✅ 지원 |
| 🟦 Naver | `NAVER_{id}` | ✅ 지원 |

---

## ⚡ **빠른 실행**

### **1. 서비스 시작**
```bash
# Docker Compose로 전체 실행 (권장)
docker-compose up -d

# 또는 로컬 개발
./gradlew bootRun
```

### **2. 서비스 확인**
| 서비스 | URL | 용도 |
|--------|-----|------|
| 🌐 Main API | http://localhost:8080 | REST API 서버 |
| 📚 Swagger | http://localhost:8080/api-docs | API 문서 |
| 🧪 Mock Login | http://localhost:8080/api/dev/auth/mock-login | 개발용 로그인 |
| 🗄️ Database | localhost:5432 | PostgreSQL (HabiDB) |

---

## 🏗️ **기술 스택 & 아키텍처**

### **기술 스택**
| 분류 | 기술 | 버전 | 역할 |
|------|------|------|------|
| **Language** | Java | 21 | 메인 개발 언어 |
| **Framework** | Spring Boot | 3.5.5 | 백엔드 프레임워크 |
| **Security** | Spring Security | 6.x | 보안 및 인증 |
| **Database** | PostgreSQL | 15 | 메인 데이터베이스 |
| **ORM** | JPA/Hibernate | - | 데이터 접근 계층 |
| **Build** | Gradle | 8.5 | 빌드 도구 |
| **Container** | Docker | - | 컨테이너화 |
| **Documentation** | Swagger/OpenAPI | 3 | API 문서화 |

### **클라이언트 기반 소셜 인증 플로우**
```
1. 클라이언트: 소셜 제공업체(Google/Naver/Kakao)에서 OAuth2 처리
2. 클라이언트: 소셜 액세스 토큰 획득
3. 클라이언트 → 서버: POST /api/auth/social/login (소셜 토큰 전송)
4. 서버: 소셜 토큰 검증 및 사용자 정보 추출
5. 서버: socialUniqueId 기반 회원 생성/조회
6. 서버: JWT 토큰 발급 (Access + Refresh)
7. 클라이언트: JWT 토큰으로 API 인증
```

---

## 🔐 **인증 시스템**

### **socialUniqueId 기반 사용자 관리**
Habiglow는 기존의 이메일 기반 인증 대신 **플랫폼별 완전 분리** 정책을 사용합니다.

```
전통적 방식: email@example.com (플랫폼 구분 불가)
↓
Habiglow 방식: KAKAO_123456789, NAVER_987654321, GOOGLE_abcdef123
```

### **JWT 토큰 구조**
| 토큰 타입 | 만료시간 | 저장위치 | 용도 |
|-----------|----------|----------|------|
| Access Token | 1시간 | Authorization Header | API 인증 |
| Refresh Token | 24시간 | HttpOnly Cookie + DB | 토큰 갱신 |

**토큰 페이로드 예시:**
```json
{
  "sub": "KAKAO_123456789",
  "socialUniqueId": "KAKAO_123456789", 
  "email": "user@example.com",
  "name": "홍길동",
  "socialType": "KAKAO",
  "iat": 1640995200,
  "exp": 1640998800
}
```

### **클라이언트 기반 소셜 로그인 API**
| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/auth/social/login` | POST | 클라이언트 소셜 토큰으로 JWT 발급 |
| `/api/auth/token/refresh` | POST | Access Token 재발급 |
| `/api/auth/logout` | POST | 로그아웃 및 토큰 무효화 |

### **개발용 Mock API** (dev 프로파일)
```bash
# 빠른 테스트를 위한 가짜 소셜 로그인
POST /api/dev/auth/mock-login
{
  "email": "test@hackathon.com",
  "name": "해커톤참가자",
  "socialType": "KAKAO",
  "mockSocialId": "hackathon_user_001"
}
```

---

## 📁 **프로젝트 구조**

### **프로젝트 구조 (Domain-Driven Design)**
```
habiglow/
├── 📁 src/main/java/com/groomthon/habiglow/
│   ├── 🚀 HabiglowApplication.java
│   ├── 📁 domain/
│   │   ├── 🔐 auth/                     # 인증 도메인
│   │   │   ├── controller/              # 토큰 관리 API
│   │   │   ├── service/                 # 인증 비즈니스 로직
│   │   │   ├── entity/                  # RefreshToken, BlacklistedToken
│   │   │   └── dto/                     # TokenResponse, MockLoginRequest
│   │   └── 👤 member/                   # 사용자 도메인
│   │       ├── controller/              # 사용자 CRUD API
│   │       ├── service/                 # 사용자 비즈니스 로직
│   │       ├── entity/                  # MemberEntity (socialUniqueId 포함)
│   │       └── dto/                     # MemberResponse
│   └── 🌐 global/
│       ├── config/                      # Security, OAuth2, Swagger 설정
│       ├── jwt/                         # JWT 토큰 관리
│       ├── oauth2/                      # OAuth2 소셜 로그인
│       │   ├── strategy/                # Google, Naver, Kakao 전략
│       │   ├── userInfo/                # 제공업체별 사용자 정보
│       │   └── handler/                 # 로그인 성공/실패 핸들러
│       ├── response/                    # 통합 응답 처리
│       └── exception/                   # 전역 예외 처리
├── 📁 src/main/resources/
│   ├── application.yml                  # 메인 설정
│   ├── application-dev.yml              # 개발환경 (Mock API 활성화)
│   ├── application-prod.yml             # 운영환경
│   └── application-oauth.yml            # OAuth2 클라이언트 설정
├── 🐳 docker-compose.yml                # PostgreSQL + App 컨테이너
├── 📋 build.gradle                      # Gradle 빌드 설정
└── 📚 문서/
    ├── README.md                        # 이 파일
    ├── API_SPECIFICATION.md             # API 명세서
    └── CLAUDE.md                        # 개발자 가이드
```

**핵심 패키지 설명:**
- `domain.auth`: JWT 토큰 발급/갱신/무효화 처리
- `domain.member`: socialUniqueId 기반 사용자 관리
- `global.oauth2`: 소셜 로그인 Strategy Pattern 구현
- `global.jwt`: JWT 생성/검증 및 보안 필터

---

### **클라이언트 소셜 로그인 테스트**
Postman 등으로 아래 API 호출:
```http
POST http://localhost:8080/api/auth/social/login
Content-Type: application/json

{
  "socialAccessToken": "클라이언트에서_받은_소셜_토큰",
  "socialType": "GOOGLE" // GOOGLE, KAKAO, NAVER
}
```

### **환경변수 설정 (선택)**
실제 소셜 로그인을 사용하려면:
```bash
# OAuth2 클라이언트 설정
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
# ... 기타
```

---

## 📚 **문서 & 참고자료**

### **📖 프로젝트 문서**
| 문서 | 설명 | 링크 |
|------|------|------|
| **API 명세서** | 전체 API 엔드포인트 및 사용법 | [API_SPECIFICATION.md](./API_SPECIFICATION.md) |
| **Swagger UI** | 인터랙티브 API 테스트 | http://localhost:8080/api-docs |

---

## 🛠️ **개발 컨벤션**

### **1. 커밋 컨벤션**

커밋 메시지는 다음 규칙을 따릅니다:

```
타입(모듈): 메시지 내용
```

- **타입** (소문자):
    - feat: 새로운 기능 추가
    - fix: 버그 수정
    - docs: 문서 수정
    - style: 코드 포맷팅, 세미콜론 누락 등 (코드 변경 없음)
    - refactor: 리팩토링 (기능 추가/변경 없음)
    - test: 테스트 코드 추가/수정
    - chore: 빌드 작업, 의존성 관리 등 기타 작업

예시:

```
feat(member): 회원 가입 API 구현
fix(auth): 토큰 만료 시간 오류 수정
docs(api): Swagger 문서 업데이트
```

### **2. 코드 포맷터**

- [NAVER Intellij Java Formatter](https://github.com/naver/hackday-conventions-java/blob/master/rule-config/naver-intellij-formatter.xml)
- [적용법](https://eroul-ri.tistory.com/26)

### **3. 브랜치 컨벤션**

브랜치는 이슈 번호와 연결하여 생성

형식: [이슈번호]-feature/설명, [이슈번호]-fix/설명 등

- 이슈 생성 -> development -> create a branch

예시:

```
23-feature/member-registration

45-hotfix/login-validation

67-chore/swagger-setup
```

### **4. 네이밍 규칙**

```java
// 클래스: PascalCase
public class UserApplicationService { }

// 메서드: camelCase (동사로 시작)
public void createUser() { }
public boolean isActive() { }

// 상수: UPPER_SNAKE_CASE
public static final String DEFAULT_ROLE = "USER";

// 패키지: 소문자 + 점
com.company.project.domain.user
```

### **5. DTO 네이밍**

```java
// Request: 동사 + 도메인명 + Request
CreateUserRequest, UpdateUserRequest

// Response: 도메인명 + Response
UserResponse, UserListResponse
```

### **6. API 응답 규격**

모든 API는 `CommonApiResponse<T>` 구조를 사용:

```java
@Data
@AllArgsConstructor
public class CommonApiResponse<T> {
    private String code;     // 응답 코드 (성공: S###, 실패: E###)
    private String message;  // 사용자 친화적 메시지
    private T data;         // 실제 응답 데이터 (성공시), 에러 상세정보 (실패시)
}
```

**성공 응답 예시:**
```json
{
  "code": "S200",
  "message": "성공",
  "data": {
    // 실제 응답 데이터
  }
}
```

**실패 응답 예시:**
```json
{
  "code": "E400",
  "message": "잘못된 입력값입니다",
  "data": [
    {
      "key": "email",
      "value": "invalid-email",
      "reason": "올바른 이메일 형식이 아닙니다."
    }
  ]
}
```

**응답 코드 체계:**
- **S2xx**: 성공 (S200, S201, S202 등)
- **E4xx**: 클라이언트 에러 (E400, E401, E404 등)
- **E5xx**: 서버 에러 (E500 등)

---

**🌟 Habiglow - 해커톤 팀 프로젝트**  
*점진적 과부하와 공감 중점의 루틴 서비스로 건강한 습관 형성을 돕는 플랫폼*