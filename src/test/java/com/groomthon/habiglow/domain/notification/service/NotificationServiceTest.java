package com.groomthon.habiglow.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.SendResponse;
import com.groomthon.habiglow.domain.notification.dto.request.RegisterTokenRequest;
import com.groomthon.habiglow.domain.notification.entity.NotificationToken;
import com.groomthon.habiglow.domain.notification.enums.PushPlatform;
import com.groomthon.habiglow.domain.notification.repository.NotificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTokenRepository tokenRepo;
    
    @Mock
    private FirebaseApp firebaseApp;
    
    @Mock
    private FirebaseMessaging firebaseMessaging;
    
    @InjectMocks
    private NotificationService notificationService;

    private NotificationToken testToken;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_DEVICE_ID = "test-device-123";
    private static final String TEST_FCM_TOKEN = "fake-fcm-token-for-testing";

    @BeforeEach
    void setUp() {
        testToken = new NotificationToken();
        testToken.setUserId(TEST_USER_ID);
        testToken.setDeviceId(TEST_DEVICE_ID);
        testToken.setToken(TEST_FCM_TOKEN);
        testToken.setPlatform(PushPlatform.ANDROID);
        testToken.setActive(true);
        testToken.setLastSeenAt(LocalDateTime.now());
    }

    @Test
    void registerToken_새로운_토큰_등록_성공() {
        // Given
        RegisterTokenRequest request = new RegisterTokenRequest();
        request.setDeviceId(TEST_DEVICE_ID);
        request.setToken(TEST_FCM_TOKEN);
        
        when(tokenRepo.findByUserIdAndDeviceId(TEST_USER_ID, TEST_DEVICE_ID))
                .thenReturn(Optional.empty());
        when(tokenRepo.save(any(NotificationToken.class))).thenReturn(testToken);

        // When
        notificationService.registerToken(TEST_USER_ID, request);

        // Then
        verify(tokenRepo).findByUserIdAndDeviceId(TEST_USER_ID, TEST_DEVICE_ID);
        verify(tokenRepo).save(argThat(token -> 
            token.getUserId().equals(TEST_USER_ID) &&
            token.getDeviceId().equals(TEST_DEVICE_ID) &&
            token.getToken().equals(TEST_FCM_TOKEN) &&
            token.getPlatform() == PushPlatform.ANDROID &&
            token.isActive()
        ));
    }

    @Test
    void registerToken_기존_토큰_업데이트_성공() {
        // Given
        RegisterTokenRequest request = new RegisterTokenRequest();
        request.setDeviceId(TEST_DEVICE_ID);
        request.setToken("new-fcm-token");
        
        NotificationToken existingToken = new NotificationToken();
        existingToken.setUserId(TEST_USER_ID);
        existingToken.setDeviceId(TEST_DEVICE_ID);
        existingToken.setToken("old-fcm-token");
        
        when(tokenRepo.findByUserIdAndDeviceId(TEST_USER_ID, TEST_DEVICE_ID))
                .thenReturn(Optional.of(existingToken));
        when(tokenRepo.save(any(NotificationToken.class))).thenReturn(existingToken);

        // When
        notificationService.registerToken(TEST_USER_ID, request);

        // Then
        verify(tokenRepo).save(argThat(token -> 
            token.getToken().equals("new-fcm-token") &&
            token.isActive()
        ));
    }

    @Test
    void deactivateByToken_성공() {
        // Given
        when(tokenRepo.findByToken(TEST_FCM_TOKEN)).thenReturn(Optional.of(testToken));

        // When
        notificationService.deactivateByToken(TEST_FCM_TOKEN);

        // Then
        assertFalse(testToken.isActive());
        verify(tokenRepo).findByToken(TEST_FCM_TOKEN);
    }

    @Test
    void sendToUser_활성_토큰이_없는_경우() throws Exception {
        // Given
        when(tokenRepo.findByActiveTrue()).thenReturn(Collections.emptyList());

        // When
        NotificationService.SendResult result = notificationService.sendToUser(
                TEST_USER_ID, "제목", "내용", null);

        // Then
        assertEquals(0, result.getSuccess());
        assertEquals(0, result.getFailure());
    }

    @Test
    void sendToUser_FCM_전송_성공() throws Exception {
        // Given
        List<NotificationToken> activeTokens = Arrays.asList(testToken);
        when(tokenRepo.findByActiveTrue()).thenReturn(activeTokens);
        
        // Mock FCM response
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);
        
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(batchResponse.getResponses()).thenReturn(Arrays.asList(successResponse));

        try (MockedStatic<FirebaseMessaging> mockFirebaseMessaging = mockStatic(FirebaseMessaging.class)) {
            mockFirebaseMessaging.when(() -> FirebaseMessaging.getInstance(firebaseApp))
                    .thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendMulticast(any())).thenReturn(batchResponse);

            // When
            Map<String, String> data = Map.of("key", "value");
            NotificationService.SendResult result = notificationService.sendToUser(
                    TEST_USER_ID, "테스트 제목", "테스트 내용", data);

            // Then
            assertEquals(1, result.getSuccess());
            assertEquals(0, result.getFailure());
            verify(firebaseMessaging).sendMulticast(any());
        }
    }

    @Test
    void sendBroadcast_전체_사용자_알림_성공() throws Exception {
        // Given
        NotificationToken token1 = createTestToken(1L, "device1", "token1");
        NotificationToken token2 = createTestToken(2L, "device2", "token2");
        List<NotificationToken> activeTokens = Arrays.asList(token1, token2);
        
        when(tokenRepo.findByActiveTrue()).thenReturn(activeTokens);
        
        // Mock FCM response
        SendResponse successResponse1 = mock(SendResponse.class);
        SendResponse successResponse2 = mock(SendResponse.class);
        when(successResponse1.isSuccessful()).thenReturn(true);
        when(successResponse2.isSuccessful()).thenReturn(true);
        
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(2);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(batchResponse.getResponses()).thenReturn(Arrays.asList(successResponse1, successResponse2));

        try (MockedStatic<FirebaseMessaging> mockFirebaseMessaging = mockStatic(FirebaseMessaging.class)) {
            mockFirebaseMessaging.when(() -> FirebaseMessaging.getInstance(firebaseApp))
                    .thenReturn(firebaseMessaging);
            when(firebaseMessaging.sendMulticast(any())).thenReturn(batchResponse);

            // When
            NotificationService.SendResult result = notificationService.sendBroadcast(
                    "전체 공지", "중요한 알림입니다", null);

            // Then
            assertEquals(2, result.getSuccess());
            assertEquals(0, result.getFailure());
        }
    }

    private NotificationToken createTestToken(Long userId, String deviceId, String token) {
        NotificationToken notificationToken = new NotificationToken();
        notificationToken.setUserId(userId);
        notificationToken.setDeviceId(deviceId);
        notificationToken.setToken(token);
        notificationToken.setPlatform(PushPlatform.ANDROID);
        notificationToken.setActive(true);
        notificationToken.setLastSeenAt(LocalDateTime.now());
        return notificationToken;
    }
}