package com.groomthon.habiglow.domain.member.entity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.global.entity.BaseTimeEntity;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
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


	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private SocialType socialType;

	@Column(nullable = true)
	private String socialId;

	@Column(unique = true, nullable = true)
	private String socialUniqueId;

	@Column(nullable = true)
	private String profileImageUrl;

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private Set<MemberInterest> interests = new HashSet<>();


	public static MemberEntity createSocialMember(String email, String name, SocialType socialType, String socialId, String profileImageUrl) {
		return MemberEntity.builder()
			.memberEmail(email)
			.memberName(name)
			.socialType(socialType)
			.socialId(socialId)
			.profileImageUrl(profileImageUrl)
			.build();
	}

	public void updateMemberName(String name) {
		this.memberName = name;
	}

	public void updateProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void updateInterests(List<RoutineCategory> categories) {
		// 기존 관심사 모두 제거 (orphanRemoval = true로 자동 삭제됨)
		this.interests.clear();
		
		// 새로운 관심사 추가 (중복 제거)
		Set<RoutineCategory> uniqueCategories = new LinkedHashSet<>(categories);
		uniqueCategories.forEach(category -> {
			MemberInterest interest = MemberInterest.of(this, category);
			this.interests.add(interest);
		});
	}

	public List<RoutineCategory> getInterestCategories() {
		return interests.stream()
			.map(MemberInterest::getCategory)
			.collect(Collectors.toList());
	}

	public boolean hasInterest(RoutineCategory category) {
		return interests.stream()
			.anyMatch(interest -> interest.getCategory() == category);
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

}