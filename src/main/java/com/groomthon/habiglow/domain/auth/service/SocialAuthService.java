package com.groomthon.habiglow.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.request.SocialLoginRequest;
import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.domain.auth.service.RefreshTokenService;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.service.MemberService;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.oauth2.service.EnhancedOAuthAttributes;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;
import com.groomthon.habiglow.global.oauth2.service.SocialApiClient;
import com.groomthon.habiglow.global.oauth2.strategy.SocialLoginStrategyManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAuthService {
    
    private final SocialApiClient socialApiClient;
    private final SocialLoginStrategyManager strategyManager;
    private final MemberService memberService;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    
    @Transactional
    public TokenResponse authenticateWithSocialToken(SocialLoginRequest request) {
        log.info("클라이언트 소셜 로그인 시도: socialType={}", request.getSocialType());
        
        // 1. 소셜 플랫폼 API로 사용자 정보 조회
        EnhancedOAuthAttributes userAttributes = socialApiClient.getUserInfo(
            request.getSocialType(), 
            request.getSocialAccessToken()
        );
        
        // 2. 사용자 조회 또는 생성
        MemberEntity member = memberService.findOrCreateSocialMember(userAttributes);
        
        // 3. JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(
            member.getId().toString(),
            member.getEmail(), 
            member.getSocialUniqueId()
        );
        
        String refreshToken = jwtUtil.createRefreshToken(
            member.getId().toString(),
            member.getEmail(), 
            member.getSocialUniqueId()
        );
        
        // 4. RefreshToken 저장
        refreshTokenService.saveRefreshToken(member.getId(), refreshToken);
        
        log.info("클라이언트 소셜 로그인 성공: memberId={}, email={}", 
                member.getId(), member.getEmail());
                
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1시간
                .refreshTokenIncluded(true)
                .build();
    }
}