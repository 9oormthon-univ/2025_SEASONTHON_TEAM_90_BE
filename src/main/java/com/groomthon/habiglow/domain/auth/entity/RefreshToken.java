package com.groomthon.habiglow.domain.auth.entity;

import java.time.LocalDateTime;

import com.groomthon.habiglow.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseTimeEntity {

	@Id
	@Column(name = "member_id")
	private String memberId;

	@Column(name = "token", columnDefinition = "TEXT", nullable = false)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	public RefreshToken update(String newToken, LocalDateTime expiresAt) {
		return RefreshToken.builder()
			.memberId(this.memberId)
			.token(newToken)
			.expiresAt(expiresAt)
			.build();
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.expiresAt);
	}
}
