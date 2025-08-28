package com.groomthon.habiglow.domain.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groomthon.habiglow.domain.auth.entity.BlacklistedToken;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {

	@Modifying
	@Query("DELETE FROM BlacklistedToken bt WHERE bt.expiresAt < :now")
	int deleteExpiredTokens(@Param("now") LocalDateTime now);
}