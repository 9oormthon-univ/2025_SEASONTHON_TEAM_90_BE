package com.groomthon.habiglow.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.dto.request.SocialLoginRequest;
import com.groomthon.habiglow.domain.auth.dto.response.TokenResponse;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.service.MemberService;
import com.groomthon.habiglow.global.jwt.JWTUtil;
import com.groomthon.habiglow.global.oauth2.dto.OAuthAttributes;
import com.groomthon.habiglow.global.oauth2.service.SocialApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAuthService {
    
    private final SocialApiClient socialApiClient;
    private final MemberService memberService;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    
    @Transactional
    public TokenResponse authenticateWithSocialToken(SocialLoginRequest request) {
        log.info("클라이언트 소셜 로그인 시도: socialType={}", request.getSocialType());

        OAuthAttributes userAttributes = socialApiClient.getUserInfo(
            request.getSocialType(), 
            request.getSocialAccessToken()
        );

        MemberEntity member = memberService.findOrCreateSocialMember(userAttributes);

        String accessToken = jwtUtil.createAccessTokenSafe(
            member.getId().toString(),
            member.getEmail(), 
            member.getSocialUniqueId()
        );
        
        String refreshToken = jwtUtil.createRefreshTokenSafe(
            member.getId().toString(),
            member.getEmail(), 
            member.getSocialUniqueId()
        );

        refreshTokenService.saveRefreshToken(member.getId(), refreshToken);
        
        log.info("클라이언트 소셜 로그인 성공: memberId={}, email={}", 
                member.getId(), member.getEmail());
                
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .refreshTokenIncluded(true)
                .build();
    }
}