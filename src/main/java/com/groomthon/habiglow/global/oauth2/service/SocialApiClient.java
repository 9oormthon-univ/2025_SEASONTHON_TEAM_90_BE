package com.groomthon.habiglow.global.oauth2.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;
import com.groomthon.habiglow.global.oauth2.strategy.SocialLoginStrategyManager;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialApiClient {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final SocialLoginStrategyManager strategyManager;
    
    public EnhancedOAuthAttributes getUserInfo(SocialType socialType, String accessToken) {
        try {
            String userInfoUrl = getUserInfoUrl(socialType);
            Map<String, Object> userAttributes = callSocialApi(userInfoUrl, accessToken);
            
            // Strategy 패턴을 사용하여 각 플랫폼별 사용자 정보 추출
            OAuthAttributes attributes = strategyManager.extractAttributes(
                socialType.name().toLowerCase(),
                getUserNameAttribute(socialType),
                userAttributes
            );
            
            // SocialType 정보를 별도로 설정 (OAuthAttributes 개선 필요)
            return new EnhancedOAuthAttributes(attributes, socialType);
            
        } catch (HttpClientErrorException e) {
            log.error("소셜 API 호출 실패: socialType={}, status={}", 
                     socialType, e.getStatusCode(), e);
            throw new BaseException(ErrorCode.INVALID_SOCIAL_TOKEN);
        } catch (Exception e) {
            log.error("소셜 사용자 정보 조회 중 오류 발생: socialType={}", socialType, e);
            throw new BaseException(ErrorCode.SOCIAL_LOGIN_ERROR);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> callSocialApi(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, Map.class
        );
        
        return response.getBody();
    }
    
    private String getUserInfoUrl(SocialType socialType) {
        return switch (socialType) {
            case GOOGLE -> "https://www.googleapis.com/oauth2/v2/userinfo";
            case KAKAO -> "https://kapi.kakao.com/v2/user/me";
            case NAVER -> "https://openapi.naver.com/v1/nid/me";
        };
    }
    
    private String getUserNameAttribute(SocialType socialType) {
        return switch (socialType) {
            case GOOGLE -> "sub";
            case KAKAO -> "id";
            case NAVER -> "response";
        };
    }
}