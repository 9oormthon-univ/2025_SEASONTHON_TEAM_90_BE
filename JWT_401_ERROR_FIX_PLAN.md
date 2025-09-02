# JWT 인증/인가 에러 중복 로그 문제 해결 계획서 (개정판)

## 1. 문제 분석

### 1.1 현상
- 하나의 401/403 인증/인가 실패 요청에 대해 다음과 같이 **여러 개의 에러 로그가 중복 발생**:
  1. JWT validation failed (JWTUtil)
  2. 토큰 검증 실패 (JwtAuthenticationFilter) 
  3. AuthorizationDeniedException (Spring Security Filter Chain)
  4. ServletException: Unable to handle Spring Security Exception (response already committed)
  5. 에러 페이지 처리 과정에서 추가 AuthorizationDeniedException
  6. 403 Forbidden 에러도 유사한 패턴으로 중복 발생 가능

### 1.2 근본 원인 분석

#### A. 아키텍처 설계 문제
현재 JWT 인증/인가 실패 처리가 **Spring Security의 표준 예외 처리 메커니즘을 우회**하고 있음:
- `JwtAuthenticationFilter`에서 직접 HTTP 응답 작성 (비표준)
- Spring Security의 `AuthenticationEntryPoint` 및 `AccessDeniedHandler` 무시
- `SecurityConfig`에서도 `response.sendError()` 직접 호출
- 필터 체인 실행 중단 없이 계속 진행

#### B. 필터 체인 흐름 문제
```
1. JwtAuthenticationFilter: 토큰 검증 실패 → 직접 응답 작성 + 커밋
2. 필터 체인 계속 실행 (filterChain.doFilter 호출)
3. AuthorizationFilter: 인증되지 않은 요청 감지 → AuthorizationDeniedException
4. ExceptionTranslationFilter: 이미 커밋된 응답으로 인한 ServletException
5. 에러 페이지 처리: 동일한 필터 체인 재실행으로 재귀적 에러
```

#### C. 응답 처리 충돌 및 중복 로직
- JWT 필터: `HttpServletResponse` 직접 조작으로 응답 커밋
- Spring Security: 표준 예외 처리로 응답 시도
- `GlobalExceptionHandler`와 필터 단의 에러 응답 생성 로직 중복
- **결과**: "response is already committed" 에러 및 비일관적 응답 형태

#### D. JWT 예외 세분화 부족
- 모든 JWT 관련 에러를 단일 에러 코드로 처리
- 토큰 만료, 서명 오류, 형식 오류 등이 구분되지 않아 클라이언트의 적절한 처리 어려움

## 2. 해결 전략

### 2.1 핵심 원칙
1. **Spring Security 표준 메커니즘 준수**: 커스텀 예외 처리 대신 Spring Security의 표준 인증/인가 예외 처리 사용
2. **단일 책임 원칙**: 각 컴포넌트가 명확한 역할 분담
3. **필터 체인 무결성**: 에러 발생 시 적절한 필터 체인 중단
4. **응답 일관성**: 모든 인증/인가 에러에 대해 통일된 응답 형태 제공
5. **예외 세분화**: JWT 관련 다양한 에러 상황을 구체적으로 분류하여 클라이언트 처리 지원

### 2.2 설계 변경 방향

#### 현재 방식 (Anti-Pattern):
```
JWT Filter → 직접 에러 응답 작성 → 필터 체인 계속 → 중복 에러
SecurityConfig → response.sendError() 직접 호출 → 일관성 부족
```

#### 변경될 방식 (Best Practice):
```
JWT Filter → 세분화된 Spring Security Exception 발생 → 
AuthenticationEntryPoint/AccessDeniedHandler → 
공통 응답 유틸리티 → 단일 일관된 에러 응답
```

### 2.3 아키텍처 개선 목표
1. **401 인증 실패**: `AuthenticationEntryPoint`로 통합 처리
2. **403 인가 실패**: `AccessDeniedHandler`로 통합 처리  
3. **응답 생성 통합**: `SecurityResponseUtils` 유틸리티로 중복 제거
4. **JWT 예외 세분화**: 토큰 만료, 서명 오류, 형식 오류 등 구분 처리

## 3. 구체적 수정 계획

### 3.1 Phase 0: 공통 응답 유틸리티 생성

#### SecurityResponseUtils 유틸리티 클래스
```java
@Component
public class SecurityResponseUtils {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(errorCode);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        
        response.getWriter().write(jsonResponse);
    }
}
```

### 3.2 Phase 1: 세분화된 JWT 예외 클래스 생성

#### JWT 예외 타입별 분류
```java
public abstract class JwtAuthenticationException extends AuthenticationException {
    private final ErrorCode errorCode;
    
    public JwtAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() { return errorCode; }
}

// 토큰 만료
public class ExpiredJwtAuthenticationException extends JwtAuthenticationException {
    public ExpiredJwtAuthenticationException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}

// 서명 오류
public class InvalidJwtSignatureException extends JwtAuthenticationException {
    public InvalidJwtSignatureException() {
        super(ErrorCode.INVALID_TOKEN_SIGNATURE);
    }
}

// 형식 오류
public class MalformedJwtException extends JwtAuthenticationException {
    public MalformedJwtException() {
        super(ErrorCode.MALFORMED_TOKEN);
    }
}

// 블랙리스트
public class BlacklistedJwtException extends JwtAuthenticationException {
    public BlacklistedJwtException() {
        super(ErrorCode.TOKEN_BLACKLISTED);
    }
}
```

### 3.3 Phase 2: JwtAuthenticationFilter 리팩토링

#### 수정된 코드 구조 (Gemini 피드백 반영)
```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
    throws ServletException, IOException {
    
    Optional<String> token = jwtUtil.extractAccessToken(request);
    
    if (token.isPresent()) {
        try {
            validateAndSetAuthentication(token.get());
        } catch (JwtAuthenticationException e) {
            // Spring Security가 처리하도록 예외 전파 (request.setAttribute 제거)
            SecurityContextHolder.clearContext();
            throw e; // 직접 던져서 AuthenticationEntryPoint가 처리하도록
        }
    }
    
    filterChain.doFilter(request, response);
}

private void validateAndSetAuthentication(String token) {
    // JWT 예외 세분화
    if (!jwtUtil.validateToken(token, "access")) {
        throw determineJwtException(token); // 예외 타입 세분화
    }
    
    if (blacklistService.isBlacklisted(token)) {
        throw new BlacklistedJwtException();
    }
    
    // 인증 설정
    setAuthentication(token);
}
```

### 3.4 Phase 3: AuthenticationEntryPoint 및 AccessDeniedHandler 구현

#### AuthenticationEntryPoint
```java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityResponseUtils responseUtils;
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, 
                        AuthenticationException authException) throws IOException {
        
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        
        // JWT 예외 타입별 분기 처리 (Gemini 제안 반영)
        if (authException instanceof JwtAuthenticationException jwtException) {
            errorCode = jwtException.getErrorCode();
        }
        
        responseUtils.sendErrorResponse(response, errorCode);
    }
}
```

#### AccessDeniedHandler (Gemini 피드백 반영)
```java
@Component  
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityResponseUtils responseUtils;
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        responseUtils.sendErrorResponse(response, ErrorCode.ACCESS_DENIED);
    }
}
```

### 3.5 Phase 4: SecurityConfig 전면 개선

#### SecurityConfig 수정
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
    JwtAuthenticationFilter jwtAuthenticationFilter,
    RateLimitFilter rateLimitFilter,
    JwtAuthenticationEntryPoint authenticationEntryPoint,
    JwtAccessDeniedHandler accessDeniedHandler) throws Exception {

    http
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(securityProperties.getPublicUrlsArray()).permitAll()
            .requestMatchers("/error").permitAll() // 에러 페이지 재귀 방지
            .anyRequest().authenticated())
        .exceptionHandling(except -> except
            .authenticationEntryPoint(authenticationEntryPoint) // 커스텀 EntryPoint
            .accessDeniedHandler(accessDeniedHandler)); // 커스텀 AccessDeniedHandler

    http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

// 성능 최적화 (Gemini 제안 반영)
@Bean
public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring()
        .requestMatchers("/favicon.ico", "/css/**", "/js/**", "/images/**")
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health");
}
```

## 4. 구현 단계별 계획 (개정)

### 4.1 0단계: 공통 유틸리티 및 예외 클래스 구현 (1일)
- [ ] `SecurityResponseUtils` 유틸리티 클래스 생성
- [ ] 세분화된 JWT 예외 클래스 생성 (`JwtAuthenticationException` 계층)
  - [ ] `ExpiredJwtAuthenticationException`
  - [ ] `InvalidJwtSignatureException` 
  - [ ] `MalformedJwtException`
  - [ ] `BlacklistedJwtException`
- [ ] 예외 클래스별 단위 테스트 작성
- [ ] `ErrorCode` enum에 새로운 에러 코드 추가 (필요시)

### 4.2 1단계: AuthenticationEntryPoint 및 AccessDeniedHandler 구현 (0.5일)
- [ ] `JwtAuthenticationEntryPoint` 구현
- [ ] `JwtAccessDeniedHandler` 구현 (Gemini 피드백 반영)
- [ ] JWT 예외 타입별 분기 처리 로직 구현
- [ ] EntryPoint/Handler 단위 테스트 작성

### 4.3 2단계: JwtAuthenticationFilter 리팩토링 (1일)  
- [ ] `sendErrorResponse` 메서드 제거
- [ ] `doFilterInternal` 메서드 재작성
- [ ] `determineJwtException` 메서드 구현 (JWT 예외 타입 판별)
- [ ] `validateAndSetAuthentication` 메서드 분리
- [ ] 예외 기반 에러 처리로 변경
- [ ] 필터 단위 테스트 강화

### 4.4 3단계: SecurityConfig 전면 개선 (0.5일)
- [ ] 커스텀 AuthenticationEntryPoint 등록
- [ ] 커스텀 AccessDeniedHandler 등록  
- [ ] 에러 페이지 재귀 방지 설정 (`/error` 경로 허용)
- [ ] `WebSecurityCustomizer` 추가 (성능 최적화)
- [ ] SecurityConfig 통합 테스트 작성

### 4.5 4단계: 통합 테스트 및 검증 (1일)
- [ ] **단일 로그 검증**: 하나의 401/403 요청 = 하나의 에러 로그
- [ ] **API 응답 일관성**: 모든 인증/인가 에러가 `CommonApiResponse` 형태
- [ ] **JWT 예외 세분화**: 클라이언트가 토큰 만료 vs 서명 오류 구분 가능
- [ ] **성능 영향 측정**: 응답 시간 변화 확인
- [ ] **스트레스 테스트**: 동시 다발적 401 에러 상황에서 로그 중복 없음 확인

## 5. 예상 효과

### 5.1 문제 해결 효과
1. **로그 중복 제거**: 하나의 401 에러 = 하나의 로그 라인
2. **Spring Security 표준 준수**: 유지보수성 및 확장성 향상
3. **응답 일관성**: 모든 인증 에러가 동일한 형태로 응답

### 5.2 사이드 이펙트 방지
- [ ] 기존 API 스펙 호환성 유지
- [ ] 클라이언트 영향도 최소화
- [ ] 성능 저하 없음 확인

## 6. 롤백 계획

### 6.1 위험 요소
- 클라이언트에서 의존하는 에러 응답 형태 변경 가능성
- 필터 체인 변경으로 인한 예상치 못한 사이드 이펙트

### 6.2 롤백 전략
1. **Feature Flag**: 새로운 에러 처리 방식을 설정으로 제어
2. **점진적 배포**: 스테이징 환경 먼저 적용
3. **모니터링**: 에러율 및 응답 시간 지속 모니터링

## 7. 검증 기준

### 7.1 성공 기준
- [ ] 단일 401 요청 → 단일 에러 로그
- [ ] "response is already committed" 에러 제거
- [ ] API 응답 형태 일관성 유지
- [ ] 성능 저하 없음 (응답 시간 ±5% 이내)

### 7.2 테스트 시나리오 (MockMvc 기반 구체화)

#### 7.2.1 JWT 인증 실패 시나리오
```java
@Test
public void testExpiredToken_ShouldReturn401WithSpecificErrorCode() throws Exception {
    String expiredToken = jwtTestUtils.createExpiredToken();
    
    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("E4011")) // TOKEN_EXPIRED
            .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다"))
            .andExpect(jsonPath("$.data").isEmpty());
}

@Test  
public void testInvalidSignature_ShouldReturn401WithSpecificErrorCode() throws Exception {
    String invalidToken = jwtTestUtils.createInvalidSignatureToken();
    
    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + invalidToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("E4012")) // INVALID_TOKEN_SIGNATURE
            .andExpect(jsonPath("$.message").value("유효하지 않은 토큰 서명입니다"));
}

@Test
public void testMalformedToken_ShouldReturn401WithSpecificErrorCode() throws Exception {
    String malformedToken = "invalid.jwt.format";
    
    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + malformedToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("E4013")) // MALFORMED_TOKEN
            .andExpect(jsonPath("$.message").value("잘못된 형식의 토큰입니다"));
}

@Test
public void testBlacklistedToken_ShouldReturn401WithSpecificErrorCode() throws Exception {
    String blacklistedToken = jwtTestUtils.createValidTokenThenBlacklist();
    
    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + blacklistedToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("E4014")) // TOKEN_BLACKLISTED
            .andExpect(jsonPath("$.message").value("블랙리스트에 등록된 토큰입니다"));
}

@Test
public void testMissingToken_ShouldReturn401WithGenericErrorCode() throws Exception {
    mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized())
            .andExpected(jsonPath("$.code").value("E401")) // UNAUTHORIZED
            .andExpect(jsonPath("$.message").value("인증이 필요합니다"));
}
```

#### 7.2.2 인가 실패 시나리오 (403)
```java
@Test
public void testAccessDenied_ShouldReturn403WithConsistentFormat() throws Exception {
    String validUserToken = jwtTestUtils.createValidUserToken();
    
    mockMvc.perform(delete("/api/admin/users/1")
            .header("Authorization", "Bearer " + validUserToken))
            .andExpect(status().isForbidden())
            .andExpected(jsonPath("$.code").value("E403")) // ACCESS_DENIED
            .andExpect(jsonPath("$.message").value("접근 권한이 없습니다"))
            .andExpect(jsonPath("$.data").isEmpty());
}
```

#### 7.2.3 로그 중복 검증 시나리오
```java
@Test
public void testSingleErrorLog_WhenAuthenticationFails() throws Exception {
    LogCaptor logCaptor = LogCaptor.forClass(JwtAuthenticationFilter.class);
    
    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer invalid-token"));
    
    // 로그가 한 번만 발생했는지 검증
    assertThat(logCaptor.getErrorLogs()).hasSize(1);
    assertThat(logCaptor.getWarnLogs()).hasSize(1); // JWT validation warning
    assertThat(logCaptor.getInfoLogs()).isEmpty(); // 성공 로그 없음
}
```

#### 7.2.4 동시성 테스트 시나리오
```java
@Test
public void testConcurrentAuthFailures_ShouldNotCauseLogDuplication() throws Exception {
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    LogCaptor logCaptor = LogCaptor.forRoot();
    
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                mockMvc.perform(get("/api/users")
                    .header("Authorization", "Bearer invalid-token-" + Thread.currentThread().getId()));
            } catch (Exception e) {
                // ignore
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(5, TimeUnit.SECONDS);
    
    // 각 요청마다 정확히 하나의 에러 로그만 발생했는지 검증
    List<String> errorLogs = logCaptor.getErrorLogs();
    assertThat(errorLogs).hasSize(threadCount);
    assertThat(errorLogs).noneMatch(log -> log.contains("response is already committed"));
}
```

#### 7.2.5 성능 영향 측정 시나리오
```java
@Test
public void testAuthenticationPerformance_ShouldNotDegrade() throws Exception {
    String validToken = jwtTestUtils.createValidToken();
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < 1000; i++) {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }
    
    long endTime = System.currentTimeMillis();
    long averageResponseTime = (endTime - startTime) / 1000;
    
    // 평균 응답 시간이 10ms 이하여야 함 (기준은 프로젝트에 맞게 조정)
    assertThat(averageResponseTime).isLessThan(10);
}
```

---

**작성일**: 2025-09-02 (개정: 2025-09-02)  
**담당자**: Backend Team  
**리뷰어**: Gemini (AI Code Reviewer)  
**우선순위**: High (로그 성능 및 모니터링 영향)  
**예상 작업 시간**: 3-4일 (세분화된 예외 처리 및 AccessDeniedHandler 추가로 증가)

## 8. 개정 이력

### v2.0 (2025-09-02)
- Gemini 리뷰 피드백 반영
- AccessDeniedHandler 추가 (403 에러 처리)
- JWT 예외 세분화 (토큰 만료, 서명 오류, 형식 오류 등 구분)
- SecurityResponseUtils 유틸리티 클래스로 중복 코드 제거
- MockMvc 기반 구체적인 테스트 시나리오 작성
- WebSecurityCustomizer를 통한 성능 최적화 추가
- request.setAttribute 불필요한 코드 제거

### v1.0 (2025-09-02)  
- 초기 계획서 작성
- JWT 401 에러 중복 로그 문제 분석 및 해결책 제시