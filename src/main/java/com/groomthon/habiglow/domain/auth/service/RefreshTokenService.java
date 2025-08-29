package com.groomthon.habiglow.domain.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.entity.RefreshToken;
import com.groomthon.habiglow.domain.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${jwt.refresh-token-expiration:86400000}")
	private long refreshTokenExpiration;

	/**
	 * Refresh Token 저장 또는 업데이트
	 */
	@Transactional
	public void saveRefreshToken(Long memberId, String token) {
		saveToken(String.valueOf(memberId), token);
	}

	@Transactional
	public void saveToken(String memberId, String token) {
		LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);

		Optional<RefreshToken> existingToken = refreshTokenRepository.findById(memberId);

		if (existingToken.isPresent()) {
			RefreshToken updatedToken = existingToken.get().update(token, expiresAt);
			refreshTokenRepository.save(updatedToken);
			log.debug("Refresh token updated for member: {}", memberId);
		} else {
			RefreshToken newToken = RefreshToken.builder()
				.memberId(memberId)
				.token(token)
				.expiresAt(expiresAt)
				.build();
			refreshTokenRepository.save(newToken);
			log.debug("New refresh token saved for member: {}", memberId);
		}
	}

	/**
	 * 회원 ID로 Refresh Token 조회
	 */
	public Optional<RefreshToken> findTokenByMemberId(String memberId) {
		Optional<RefreshToken> token = refreshTokenRepository.findById(memberId);

		if (token.isPresent() && token.get().isExpired()) {
			deleteRefreshToken(memberId);
			return Optional.empty();
		}

		return token;
	}

	/**
	 * 회원 ID로 유효한 Refresh Token 존재 여부 확인
	 */
	public boolean existsByMemberId(String memberId) {
		Optional<RefreshToken> token = refreshTokenRepository.findById(memberId);
		return token.isPresent() && !token.get().isExpired();
	}

	/**
	 * Refresh Token 삭제
	 */
	@Transactional
	public void deleteRefreshToken(Long memberId) {
		deleteRefreshToken(String.valueOf(memberId));
	}

	@Transactional
	public void deleteRefreshToken(String memberId) {
		refreshTokenRepository.deleteById(memberId);
		log.debug("Refresh token deleted for member: {}", memberId);
	}

	/**
	 * 만료된 토큰들을 주기적으로 정리 (매일 새벽 2시)
	 */
	@Scheduled(cron = "0 0 2 * * ?")
	@Transactional
	public void cleanupExpiredTokens() {
		int deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
		log.info("Cleaned up {} expired refresh tokens", deletedCount);
	}
}