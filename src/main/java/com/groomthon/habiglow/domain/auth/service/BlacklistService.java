package com.groomthon.habiglow.domain.auth.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.auth.entity.BlacklistedToken;
import com.groomthon.habiglow.domain.auth.repository.BlacklistedTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlacklistService {

	private final BlacklistedTokenRepository blacklistedTokenRepository;

	/**
	 * Access Token을 블랙리스트에 추가
	 */
	@Transactional
	public void addToBlacklist(String accessToken, long expirationMillis) {
		String tokenHash = hashToken(accessToken);
		LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMillis / 1000);

		BlacklistedToken blacklistedToken = BlacklistedToken.builder()
			.tokenHash(tokenHash)
			.expiresAt(expiresAt)
			.build();

		blacklistedTokenRepository.save(blacklistedToken);
		log.info("블랙리스트 등록: {}, 만료 시간: {}", tokenHash.substring(0, 10) + "...", expiresAt);
	}

	/**
	 * Access Token이 블랙리스트에 존재하는지 확인
	 */
	public boolean isBlacklisted(String accessToken) {
		String tokenHash = hashToken(accessToken);
		Optional<BlacklistedToken> token = blacklistedTokenRepository.findById(tokenHash);

		if (token.isEmpty()) {
			return false;
		}

		// 만료된 토큰인 경우 삭제하고 false 반환
		if (token.get().isExpired()) {
			blacklistedTokenRepository.deleteById(tokenHash);
			return false;
		}

		log.debug("블랙리스트 조회: {}, 결과: true", tokenHash.substring(0, 10) + "...");
		return true;
	}

	/**
	 * 토큰을 SHA-256으로 해시화
	 */
	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes());
			StringBuilder hexString = new StringBuilder();

			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			log.error("SHA-256 algorithm not available", e);
			throw new RuntimeException("Token hashing failed", e);
		}
	}

	/**
	 * 만료된 블랙리스트 토큰들을 주기적으로 정리 (매일 새벽 3시)
	 */
	@Scheduled(cron = "0 0 3 * * ?")
	@Transactional
	public void cleanupExpiredTokens() {
		int deletedCount = blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
		log.info("Cleaned up {} expired blacklisted tokens", deletedCount);
	}
}