package com.groomthon.habiglow.domain.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.groomthon.habiglow.domain.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

	@Modifying
	@Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
	int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
