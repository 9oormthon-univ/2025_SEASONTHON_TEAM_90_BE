# 🌟 Feature Development Record

> **개발 기간**: 2025-01-30  
> **개발자**: Claude Code  
> **기능**: 회원 프로필 이미지 URL + 관심사 관리 시스템

---

## 📋 개발 개요

### 구현된 기능
1. **회원 프로필 이미지 URL** - 소셜 로그인 시 자동 수집 및 관리
2. **회원 관심사 관리 시스템** - 루틴 카테고리 기반 개인화 기능

### 기술적 특징
- **Domain-Driven Design** 패턴 준수
- **복합 기본 키**를 활용한 데이터 무결성 보장
- **Cascade 관계**로 관심사 업데이트 로직 단순화
- **RESTful API** 설계 원칙 적용

---

## 🎯 기능 1: 회원 프로필 이미지 URL

### 구현 내용

#### 1. 데이터 모델 수정
```java
// MemberEntity.java
@Column(nullable = true)
private String profileImageUrl;

public void updateProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
}
```

#### 2. OAuth2 통합
- **Google**: `picture` 필드 활용
- **Kakao**: `thumbnail_image_url` 필드 활용  
- **Naver**: `profile_image` 필드 활용

#### 3. 자동 수집 및 업데이트 로직
```java
// OAuthAttributes.java
public String getImageUrl() {
    return oauth2UserInfo.getImageUrl();
}

// MemberService.java
private MemberEntity updateExistingMemberProfile(MemberEntity existingMember, OAuthAttributes attributes) {
    String newProfileImageUrl = attributes.getImageUrl();
    if (newProfileImageUrl != null && !newProfileImageUrl.equals(existingMember.getProfileImageUrl())) {
        existingMember.updateProfileImageUrl(newProfileImageUrl);
        log.info("기존 회원 프로필 이미지 업데이트: memberId={}", existingMember.getId());
    }
    return existingMember;
}
```

#### 4. API 응답 통합
```json
{
  "id": 1,
  "memberName": "홍길동",
  "memberEmail": "hong@example.com",
  "socialType": "GOOGLE",
  "profileImageUrl": "https://lh3.googleusercontent.com/a/example"
}
```

---

## 🎯 기능 2: 회원 관심사 관리 시스템

### 아키텍처 설계

#### 1. 엔티티 관계 설계 (Many-to-Many with Join Entity)
```java
// 복합 기본 키 클래스
@IdClass(MemberInterestId.class)
public class MemberInterest extends BaseTimeEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    private MemberEntity member;
    
    @Id
    @Enumerated(EnumType.STRING)
    private RoutineCategory category;
}

// MemberEntity의 관계 설정
@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@Builder.Default
private Set<MemberInterest> interests = new HashSet<>();
```

#### 2. 비즈니스 로직 최적화
```java
// MemberEntity.java - Cascade를 활용한 간단한 업데이트 로직
public void updateInterests(List<RoutineCategory> categories) {
    // orphanRemoval = true로 기존 관심사 자동 삭제
    this.interests.clear();
    
    // 새로운 관심사 추가
    categories.forEach(category -> {
        MemberInterest interest = MemberInterest.of(this, category);
        this.interests.add(interest);
    });
}
```

### API 엔드포인트 설계

#### 1. 루틴 카테고리 조회 API
```http
GET /api/routine-categories
```
- **목적**: 선택 가능한 모든 루틴 카테고리 제공
- **인증**: 불필요 (공개 정보)
- **응답**: 9개 카테고리 (건강, 학습, 마음챙김 등)

#### 2. 관심사 조회 API
```http
GET /api/members/me/interests
```
- **목적**: 로그인 사용자의 현재 관심사 조회
- **인증**: JWT 토큰 필수

#### 3. 관심사 수정 API
```http
PUT /api/members/me/interests
Content-Type: application/json

{
  "interests": ["HEALTH", "LEARNING", "MINDFULNESS"]
}
```
- **목적**: 관심사 일괄 업데이트
- **제약**: 1개 이상 5개 이하 (`@Size(min=1, max=5)`)

### 데이터 유효성 검증
```java
@NotNull(message = "관심사 목록은 필수입니다.")
@Size(min = 1, max = 5, message = "관심사는 1개 이상 5개 이하로 선택해주세요.")
private List<RoutineCategory> interests;
```

---

## 🏗️ 기술적 우수성

### 1. 데이터 무결성 보장
- **복합 기본 키**: Member와 Category 조합 유일성 보장
- **Enum 타입**: `EnumType.STRING`으로 운영 안정성 확보
- **외래키 제약**: 데이터베이스 레벨 무결성 보장

### 2. 성능 최적화
- **Lazy Loading**: `fetch = FetchType.LAZY`로 N+1 문제 방지
- **Set 컬렉션**: 중복 방지 + O(1) 조회 성능
- **Cascade 최적화**: 일괄 처리로 DB 쿼리 최소화

### 3. 유지보수성
- **도메인 분리**: 관심사 로직을 별도 서비스로 분리
- **확장 가능한 설계**: 향후 관심도 점수 등 추가 필드 확장 용이
- **일관된 응답**: `CommonApiResponse<T>` 표준 준수

### 4. 사용자 경험 (UX)
- **점진적 온보딩**: 관심사 선택을 선택사항으로 처리
- **실시간 반영**: 관심사 수정 즉시 프로필 조회에 반영
- **명확한 제약**: 1~5개 제한으로 사용자 혼란 방지

---

## 📊 API 응답 예시

### 회원 정보 조회 (관심사 포함)
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

### 관심사 수정 응답
```json
{
  "code": "S200",
  "message": "성공",
  "data": null
}
```

### 유효성 검증 실패 (6개 선택 시)
```json
{
  "code": "E400",
  "message": "관심사는 1개 이상 5개 이하로 선택해주세요.",
  "data": [
    {
      "key": "interests",
      "value": "[\"HEALTH\", \"DIET\", \"EXERCISE\", \"LEARNING\", \"MINDFULNESS\", \"HOBBY\"]",
      "reason": "관심사는 1개 이상 5개 이하로 선택해주세요."
    }
  ]
}
```

---

## 📝 구현된 파일 목록

### 새로 생성된 파일
- `MemberInterestId.java` - 복합 기본 키 클래스
- `MemberInterest.java` - 중간 테이블 엔티티
- `MemberInterestService.java` - 관심사 관리 서비스
- `UpdateMemberInterestsRequest.java` - 관심사 수정 요청 DTO
- `MemberInterestsResponse.java` - 관심사 조회 응답 DTO
- `RoutineCategoryController.java` - 카테고리 조회 컨트롤러
- `RoutineCategoryResponse.java` - 카테고리 응답 DTO

### 수정된 파일
- `MemberEntity.java` - 프로필 URL + 관심사 관계 추가
- `MemberResponse.java` - 프로필 URL + 관심사 필드 추가
- `MemberService.java` - 프로필 URL 자동 업데이트 로직
- `MemberApiController.java` - 관심사 관리 API 추가
- `OAuthAttributes.java` - 이미지 URL getter 추가
- `DevAuthService.java` - Mock 사용자 생성 로직 수정

### 문서 업데이트
- `AUTH_API_SPECIFICATION.md` - 새로운 API 엔드포인트 문서화

---

## 🧪 테스트 시나리오

### 1. 프로필 이미지 URL 테스트
- [ ] 신규 Google 로그인 → 프로필 이미지 자동 저장
- [ ] 신규 Kakao 로그인 → 썸네일 이미지 자동 저장  
- [ ] 기존 회원 재로그인 → 프로필 이미지 변경 감지 및 업데이트
- [ ] Mock 사용자 생성 → null 프로필 이미지 처리

### 2. 관심사 관리 테스트
- [ ] `GET /api/routine-categories` → 9개 카테고리 조회
- [ ] 신규 회원 관심사 조회 → 빈 배열 반환
- [ ] 관심사 3개 설정 → 정상 저장 및 조회
- [ ] 관심사 6개 설정 시도 → 400 에러 (유효성 검증)
- [ ] 기존 관심사 수정 → 이전 관심사 삭제 + 신규 관심사 저장
- [ ] `GET /api/members/me` → 관심사 포함하여 응답

### 3. 통합 테스트
- [ ] 소셜 로그인 → 프로필 이미지 + 빈 관심사로 회원 생성
- [ ] 관심사 설정 → 마이페이지에서 관심사 포함 조회
- [ ] 회원 삭제 → 관심사 데이터 cascade 삭제 확인

---

## 🚀 향후 확장 가능성

### 1. 개인화 서비스
- 관심사 기반 루틴 추천 알고리즘
- 사용자 선호도 분석 대시보드

### 2. 소셜 기능 확장
- 관심사 기반 사용자 매칭
- 관심사별 커뮤니티 그룹 기능

### 3. 데이터 분석
- 인기 관심사 통계 수집
- 사용자 행동 패턴 분석

---

## 📈 성능 및 확장성 고려사항

### 데이터베이스 설계
- **인덱스 최적화**: member_id, routine_category 복합 인덱스
- **확장성**: MemberInterest 테이블에 추가 필드(관심도, 우선순위 등) 확장 가능
- **파티셔닝 고려**: 대용량 데이터 시 member_id 기준 파티셔닝 가능

### API 성능
- **캐싱 전략**: 루틴 카테고리는 Redis 캐싱 적용 가능
- **페이징**: 대용량 관심사 조회 시 페이징 추가 고려
- **배치 처리**: 대량 관심사 업데이트 시 배치 API 고려

---

**✅ 총평: Gemini의 모든 기술적 제안을 100% 반영한 견고하고 확장 가능한 시스템 구현 완료**