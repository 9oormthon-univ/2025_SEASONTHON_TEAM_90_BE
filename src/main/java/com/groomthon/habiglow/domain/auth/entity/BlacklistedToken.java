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
@Table(name = "blacklisted_tokens")
public class BlacklistedToken extends BaseTimeEntity {

	@Id
	@Column(name = "token_hash")
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.expiresAt);
	}
}