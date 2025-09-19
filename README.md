
<img width="661" height="371" alt="스크린샷 2025-09-07 015430" src="https://github.com/user-attachments/assets/c873bd09-a31c-4daa-afd3-a8e0be86dcb0" />
<p></p>
<img width="763" height="425" alt="스크린샷 2025-09-07 015450" src="https://github.com/user-attachments/assets/03bc2a3f-cce4-4cb9-8764-bcd909a66d72" />
<p></p>

<img width="757" height="432" alt="스크린샷 2025-09-07 015458" src="https://github.com/user-attachments/assets/3eb65fd2-71b0-40df-80ac-b041cd1985c0" />
<p></p>

<img width="762" height="430" alt="스크린샷 2025-09-07 015512" src="https://github.com/user-attachments/assets/83b5df22-1454-46f8-a1c9-18161f2b396e" />
<p></p>

<img width="807" height="456" alt="스크린샷 2025-09-07 015526" src="https://github.com/user-attachments/assets/8e7424e1-3571-43a0-898e-0e121aafc0e2" />
<p></p>

<img width="809" height="451" alt="스크린샷 2025-09-07 015537" src="https://github.com/user-attachments/assets/48c42206-ed23-49fd-8731-6e3e5a939227" />
<p></p>

<img width="801" height="448" alt="스크린샷 2025-09-07 015606" src="https://github.com/user-attachments/assets/30d6e51a-5dc4-4862-af99-981e1e47201c" />
<p></p>

<img width="799" height="437" alt="스크린샷 2025-09-07 015614" src="https://github.com/user-attachments/assets/a4785f74-88cc-46df-b8da-3a4d5b6a9196" />

<p></p>
<p></p>

# 🌟 Habiglow

> **해커톤 팀 프로젝트 - 점진적 과부하와 공감 중점의 루틴 서비스**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)

**📖 API 문서**: [인증 API](api-docs/API_SPECIFICATION.md) | [루틴 API](api-docs/ROUTINE_API_SPECIFICATION.md)

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

---

### **서비스 확인**
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

## 📁 **프로젝트 구조**

### **프로젝트 구조 (Domain-Driven Design)**
```
habiglow/
├── 📁 src/main/java/com/groomthon/habiglow/
│   ├── 🚀 HabiglowApplication.java
│   ├── 📁 domain/
│   │   ├── 🔐 auth/                     # 인증 도메인
│   │   │   ├── controller/              # 토큰 관리 API (AuthApiController, DevAuthController)
│   │   │   ├── service/                 # 인증 비즈니스 로직 (AuthenticationService, RefreshTokenService 등)
│   │   │   ├── entity/                  # RefreshToken, BlacklistedToken
│   │   │   ├── repository/              # 토큰 저장소 (RefreshTokenRepository, BlacklistedTokenRepository)
│   │   │   └── dto/                     # 요청/응답 DTO (TokenResponse, MockLoginRequest, SocialLoginRequest)
│   │   │       ├── request/             # 요청 DTO
│   │   │       └── response/            # 응답 DTO
│   │   ├── 📅 daily/                    # 일간 기록 도메인
│   │   │   ├── controller/              # 일간 기록 API (DailyRecordController)
│   │   │   ├── service/                 # 일간 기록 비즈니스 로직
│   │   │   ├── entity/                  # 일간 기록 엔티티
│   │   │   ├── repository/              # 일간 기록 저장소
│   │   │   ├── facade/                  # 파사드 패턴 구현
│   │   │   ├── event/                   # 도메인 이벤트
│   │   │   ├── exception/               # 도메인별 예외
│   │   │   └── dto/                     # 일간 기록 DTO
│   │   │       ├── request/             # 요청 DTO (RoutinePerformanceRequest, SaveDailyRecordRequest)
│   │   │       ├── response/            # 응답 DTO (DailyRecordResponse)
│   │   │       └── error/               # 에러 DTO (InvalidRoutineError)
│   │   ├── 📊 dashboard/                # 대시보드 도메인
│   │   │   ├── controller/              # 대시보드 API
│   │   │   ├── service/                 # 대시보드 비즈니스 로직
│   │   │   ├── entity/                  # 대시보드 엔티티
│   │   │   ├── repository/              # 대시보드 저장소
│   │   │   ├── client/                  # 외부 클라이언트 연동
│   │   │   ├── config/                  # 대시보드 설정
│   │   │   ├── util/                    # 유틸리티 클래스
│   │   │   └── dto/                     # 대시보드 DTO
│   │   ├── 👤 member/                   # 사용자 도메인
│   │   │   ├── controller/              # 사용자 CRUD API
│   │   │   ├── service/                 # 사용자 비즈니스 로직
│   │   │   ├── entity/                  # MemberEntity (socialUniqueId 포함)
│   │   │   ├── repository/              # 사용자 저장소
│   │   │   └── dto/                     # 사용자 DTO
│   │   │       ├── request/             # 요청 DTO
│   │   │       └── response/            # 응답 DTO (MemberResponse)
│   │   ├── 🔔 notification/             # 알림 도메인
│   │   │   ├── controller/              # 알림 API
│   │   │   ├── service/                 # 알림 비즈니스 로직
│   │   │   ├── entity/                  # 알림 엔티티
│   │   │   ├── repository/              # 알림 저장소
│   │   │   ├── enums/                   # 알림 관련 열거형
│   │   │   └── dto/                     # 알림 DTO
│   │   │       ├── request/             # 요청 DTO
│   │   │       └── response/            # 응답 DTO
│   │   └── 🔄 routine/                  # 루틴 도메인
│   │       ├── controller/              # 루틴 API
│   │       ├── service/                 # 루틴 비즈니스 로직
│   │       │   └── strategy/            # 전략 패턴 구현
│   │       ├── entity/                  # 루틴 엔티티
│   │       ├── repository/              # 루틴 저장소
│   │       ├── facade/                  # 파사드 패턴 구현
│   │       ├── event/                   # 도메인 이벤트
│   │       ├── common/                  # 공통 유틸리티
│   │       └── dto/                     # 루틴 DTO
│   │           ├── request/             # 요청 DTO
│   │           └── response/            # 응답 DTO
│   │               └── adaptation/      # 적응형 응답 DTO
│   └── 🌐 global/
│       ├── config/                      # 글로벌 설정
│       │   └── properties/              # 설정 프로퍼티
│       ├── jwt/                         # JWT 토큰 관리
│       ├── oauth2/                      # OAuth2 소셜 로그인
│       │   ├── strategy/                # Google, Naver, Kakao 전략
│       │   ├── userInfo/                # 제공업체별 사용자 정보
│       │   ├── service/                 # OAuth2 서비스
│       │   ├── entity/                  # OAuth2 엔티티
│       │   └── dto/                     # OAuth2 DTO
│       ├── security/                    # 보안 설정
│       ├── response/                    # 통합 응답 처리
│       ├── exception/                   # 전역 예외 처리
│       │   └── jwt/                     # JWT 관련 예외
│       ├── advice/                      # 글로벌 어드바이스
│       ├── swagger/                     # Swagger 설정
│       ├── fcm/                         # FCM 푸시 알림
│       ├── entity/                      # 글로벌 엔티티
│       ├── dto/                         # 글로벌 DTO
│       └── util/                        # 유틸리티 클래스
├── 📁 src/main/resources/
│   ├── application.yml                  # 메인 설정
│   ├── application-dev.yml              # 개발환경 (Mock API 활성화)
│   └── application-prod.yml             # 운영환경
├── 📁 api-docs/                         # API 문서 디렉토리
│   ├── API_SPECIFICATION.md             # 인증 API 명세서
│   └── ROUTINE_API_SPECIFICATION.md     # 루틴 API 명세서
├── 🐳 docker-compose.yml                # PostgreSQL + App 컨테이너
├── 📋 build.gradle                      # Gradle 빌드 설정
└── 📚 README.md                         # 프로젝트 문서
```

## 📚 **문서 & 참고자료**

### **📖 프로젝트 문서**
| 문서 | 설명 | 링크 |
|------|------|------|
| **인증 API 명세서** | 소셜 로그인, JWT 토큰 관리 API | [API_SPECIFICATION.md](api-docs/API_SPECIFICATION.md) |
| **루틴 API 명세서** | 루틴 생성/관리, 성장 모드 API | [ROUTINE_API_SPECIFICATION.md](api-docs/ROUTINE_API_SPECIFICATION.md) |
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
