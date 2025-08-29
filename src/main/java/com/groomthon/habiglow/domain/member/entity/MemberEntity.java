package com.groomthon.habiglow.domain.member.entity;

import com.groomthon.habiglow.global.entity.BaseTimeEntity;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member_table", indexes = {
	@Index(name = "idx_member_email", columnList = "memberEmail"),
	@Index(name = "idx_social_type_id", columnList = "socialType, socialId"),
	@Index(name = "idx_social_unique_id", columnList = "socialUniqueId")
})
public class MemberEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String memberEmail;

	@Column
	private String memberName;

	@Column
	private String memberPassword;


	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private SocialType socialType;

	@Column(nullable = true)
	private String socialId;

	@Column(unique = true, nullable = true)
	private String socialUniqueId;


	public static MemberEntity createSocialMember(String email, String name, SocialType socialType, String socialId) {
		return MemberEntity.builder()
			.memberEmail(email)
			.memberName(name)
			.memberPassword(null)
			.socialType(socialType)
			.socialId(socialId)
			.build();
	}

	public void updateMemberName(String name) {
		this.memberName = name;
	}

	public void updateSocialInfo(SocialType socialType, String socialId) {
		this.socialType = socialType;
		this.socialId = socialId;
		generateSocialUniqueId();
	}

	@PrePersist
	@PreUpdate
	private void generateSocialUniqueId() {
		if (socialType != null && socialId != null) {
			this.socialUniqueId = socialType.name() + "_" + socialId;
		}
	}

	public boolean isSocialUser() {
		return socialType != null && socialId != null;
	}

	public String getEmail() {
		return memberEmail;
	}
}