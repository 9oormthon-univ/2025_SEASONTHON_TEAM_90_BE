# JWT 401 에러 분석 및 해결방안

## 📋 에러 분석

### 현재 상황
- **에러 타입**: `AuthorizationDeniedException: Access Denied`
- **발생 빈도**: 동일한 요청에 대해 3번 연속 발생
- **에러 위치**: `AuthorizationFilter.doFilter(AuthorizationFilter.java:99)`

### 주요 문제점 분석

#### 1. JWT 필터 처리 과정의 문제
현재 `JwtAuthenticationFilter`에서는 다음과 같은 문제점이 있습니다:

```java
// JwtAuthenticationFilter.java:58
filterChain.doFilter(request, response);
```

**문제**: JWT 토큰이 유효하지 않거나 존재하지 않을 때도 필터 체인을 계속 진행합니다. 이로 인해:
- 인증 실패 시에도 다음 필터(AuthorizationFilter)로 넘어감
- SecurityContext에 인증 정보가 없는 상태로 권한 검사 진행
- `AuthorizationDeniedException` 발생

#### 2. 에러 응답 중복 처리
```java
// JwtAuthenticationFilter.java:77-92
private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode)
```

**문제**: JWT 필터에서 이미 에러 응답을 작성했음에도 불구하고 필터 체인을 계속 진행하여:
- 첫 번째: JWT 필터에서 401 응답 시도
- 두 번째: AuthorizationFilter에서 `AuthorizationDeniedException` 발생
- 세 번째: ExceptionTranslationFilter에서 "response is already committed" 에러

#### 3. SecurityContext 설정 실패
인증 토큰이 없거나 유효하지 않을 때:
- `SecurityContextHolder.getContext().setAuthentication(null)` 상태
- `anyRequest().authenticated()` 설정으로 인해 모든 비공개 API에서 접근 거부

## 🔧 해결 방안

### 1. JWT 필터 로직 개선 (우선순위: 높음)

**현재 문제점**: 토큰 검증 실패 시에도 필터 체인 진행
**해결방안**: 토큰 검증 실패 시 필터 체인 중단

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    Optional<String> tokenOpt = jwtUtil.extractAccessToken(request);
    
    if (tokenOpt.isPresent()) {
        String token = tokenOpt.get();
        
        // 토큰 검증
        if (!validateAccessToken(token, response)) {
            // 검증 실패 시 필터 체인 중단
            return;
        }
        
        // 인증 정보 설정
        setAuthentication(token);
    }
    
    // 토큰이 없거나 유효한 경우에만 필터 체인 계속 진행
    filterChain.doFilter(request, response);
}
```

### 2. 에러 응답 방식 개선 (우선순위: 중간)

**현재 문제점**: JWT 필터에서 직접 응답 작성 후 필터 체인 계속 진행
**해결방안**: Spring Security의 표준 예외 처리 메커니즘 활용

```java
private void setAuthentication(String token) {
    try {
        String email = jwtUtil.getEmail(token).orElseThrow();
        String userId = jwtUtil.getId(token).orElseThrow();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userId, null, 
            Collections.singletonList(new SimpleGrantedAuthority("SOCIAL_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
    } catch (Exception e) {
        log.warn("JWT 인증 처리 실패: {}", e.getMessage());
        SecurityContextHolder.clearContext();
    }
}
```

### 3. Security Config 예외 처리 개선 (우선순위: 낮음)

**현재 상태**: 기본 `authenticationEntryPoint` 사용
**개선방안**: 커스텀 예외 처리기 구현

```java
.exceptionHandling(except -> except
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(ErrorCode.ACCESS_DENIED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    })
)
```

## 📝 구현 계획

### Phase 1: 긴급 수정 (즉시 구현)
1. **JWT 필터 로직 수정**
   - 토큰 검증 실패 시 `return` 추가하여 필터 체인 중단
   - Public URL 예외 처리 로직 확인 및 보완

### Phase 2: 안정화 (1-2일 내)
2. **예외 처리 표준화**
   - Spring Security 표준 예외 처리 메커니즘 적용
   - `AuthenticationEntryPoint` 및 `AccessDeniedHandler` 커스터마이징
   - **`sendErrorResponse` 메서드 완전 제거**: 필터가 직접 응답을 생성하지 않도록 완전 정리

### Phase 3: 최적화 (추후)
3. **로깅 및 모니터링 개선**
   - **토큰 실패 원인별 상세 로깅**: 만료/서명오류/형식오류 등 구체적 원인 로깅
   - 인증/인가 실패 통계 및 모니터링 시스템 구축
   - 보안 이벤트 추적 및 알림 시스템

## ⚠️ 주의사항

1. **Response Commit 상태 확인**: 응답이 이미 커밋된 상태에서 추가 응답 시도 금지
2. **필터 순서 유지**: JWT 필터가 Authorization 필터보다 먼저 실행되도록 순서 유지
3. **SecurityContext 정리**: 인증 실패 시 SecurityContext 명시적 초기화
4. **Public URL 확인**: 현재 요청 URL이 public URL 목록에 포함되어 있는지 확인

## 🔍 테스트 계획

1. **유효한 JWT 토큰**: 정상 인증 처리 확인
2. **유효하지 않은 JWT 토큰**: 단일 401 응답 확인
3. **토큰 없는 요청**: Public URL과 Protected URL에서의 다른 처리 확인
4. **블랙리스트 토큰**: 적절한 거부 처리 확인

## 🔄 Gemini 리뷰 피드백 반영사항

### 강점으로 평가받은 부분
- ✅ **정확한 원인 분석**: JWT 필터 체인 진행 문제와 응답 중복 처리 이슈의 정확한 파악
- ✅ **표준적인 해결방안**: Spring Security의 AuthenticationEntryPoint/AccessDeniedHandler 활용
- ✅ **체계적인 구현계획**: 긴급수정 → 안정화 → 최적화 단계별 접근
- ✅ **상세한 테스트계획**: 다양한 시나리오를 포함한 검증 방안

### 추가 개선사항 반영
- 🔧 **Phase 2에서 `sendErrorResponse` 완전 제거**: 구조적으로 더 깔끔한 설계
- 📊 **Phase 3 로깅 강화**: 토큰 실패 원인별 상세 로깅으로 문제 추적 개선
- ⚡ **Phase 1에 Public URL 예외 처리 추가**: 긴급 수정 시 놓치기 쉬운 부분 보완

---
*문서 작성일: 2025-09-02*  
*최종 업데이트: 2025-09-02 (Gemini 리뷰 반영)*  
*작성자: Claude Code Assistant*