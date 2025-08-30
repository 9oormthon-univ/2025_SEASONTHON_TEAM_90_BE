package com.groomthon.habiglow.domain.member.entity;

import com.groomthon.habiglow.domain.routine.entity.RoutineCategory;
import com.groomthon.habiglow.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "member_interest")
@IdClass(MemberInterestId.class)
public class MemberInterest extends BaseTimeEntity {
	
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private MemberEntity member;
	
	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "routine_category", nullable = false, length = 20)
	private RoutineCategory category;
	
	public static MemberInterest of(MemberEntity member, RoutineCategory category) {
		return MemberInterest.builder()
			.member(member)
			.category(category)
			.build();
	}
}