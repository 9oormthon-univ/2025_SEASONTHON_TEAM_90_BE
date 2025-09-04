package com.groomthon.habiglow.domain.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.domain.notification.dto.request.RegisterTokenRequest;
import com.groomthon.habiglow.domain.notification.dto.request.SendTestRequest;
import com.groomthon.habiglow.domain.notification.entity.NotificationToken;
import com.groomthon.habiglow.domain.notification.repository.NotificationTokenRepository;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JWTUtil jwtUtil;
    
    @Autowired
    private NotificationTokenRepository tokenRepository;

    private String accessToken;
    private final Long TEST_USER_ID = 12345L;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // JWT 토큰 생성
        accessToken = jwtUtil.createAccessToken(String.valueOf(TEST_USER_ID), TEST_EMAIL);
    }

    @Test
    void registerToken_성공() throws Exception {
        // Given
        RegisterTokenRequest request = new RegisterTokenRequest();
        request.setDeviceId("test-device-123");
        request.setToken("fake-fcm-token-for-testing");

        // When & Then
        mockMvc.perform(post("/api/notifications/tokens")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 데이터베이스 확인
        var savedToken = tokenRepository.findByUserIdAndDeviceId(TEST_USER_ID, "test-device-123");
        assertTrue(savedToken.isPresent());
        assertEquals("fake-fcm-token-for-testing", savedToken.get().getToken());
        assertTrue(savedToken.get().isActive());
    }

    @Test
    void registerToken_인증없이_요청_실패() throws Exception {
        // Given
        RegisterTokenRequest request = new RegisterTokenRequest();
        request.setDeviceId("test-device");
        request.setToken("test-token");

        // When & Then
        mockMvc.perform(post("/api/notifications/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivateToken_성공() throws Exception {
        // Given - 먼저 토큰을 등록
        NotificationToken token = new NotificationToken();
        token.setUserId(TEST_USER_ID);
        token.setDeviceId("test-device");
        token.setToken("token-to-deactivate");
        token.setActive(true);
        tokenRepository.save(token);

        // When & Then
        mockMvc.perform(delete("/api/notifications/tokens/token-to-deactivate")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // 데이터베이스 확인
        var deactivatedToken = tokenRepository.findByToken("token-to-deactivate");
        assertTrue(deactivatedToken.isPresent());
        assertFalse(deactivatedToken.get().isActive());
    }

    @Test
    void sendTest_성공_응답_구조_확인() throws Exception {
        // Given - 활성 토큰 등록
        NotificationToken token = new NotificationToken();
        token.setUserId(TEST_USER_ID);
        token.setDeviceId("test-device");
        token.setToken("fake-fcm-token");
        token.setActive(true);
        tokenRepository.save(token);

        SendTestRequest request = new SendTestRequest();
        request.setTitle("테스트 제목");
        request.setBody("테스트 내용");
        request.setData(Map.of("key", "value"));

        // When & Then
        // Firebase 실제 연결은 실패하지만, 응답 구조는 확인 가능
        mockMvc.perform(post("/api/notifications/send-test")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.failure").exists());
    }

    @Test
    void registerToken_잘못된_요청_데이터_검증() throws Exception {
        // Given - 빈 요청
        RegisterTokenRequest request = new RegisterTokenRequest();
        // deviceId와 token을 설정하지 않음

        // When & Then
        mockMvc.perform(post("/api/notifications/tokens")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 토큰_중복_등록_업데이트_확인() throws Exception {
        // Given - 첫 번째 토큰 등록
        RegisterTokenRequest firstRequest = new RegisterTokenRequest();
        firstRequest.setDeviceId("same-device");
        firstRequest.setToken("first-token");

        mockMvc.perform(post("/api/notifications/tokens")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // When - 같은 디바이스에 새 토큰 등록
        RegisterTokenRequest secondRequest = new RegisterTokenRequest();
        secondRequest.setDeviceId("same-device");
        secondRequest.setToken("updated-token");

        mockMvc.perform(post("/api/notifications/tokens")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        // Then - 토큰이 업데이트되었는지 확인
        var updatedToken = tokenRepository.findByUserIdAndDeviceId(TEST_USER_ID, "same-device");
        assertTrue(updatedToken.isPresent());
        assertEquals("updated-token", updatedToken.get().getToken());
    }
}