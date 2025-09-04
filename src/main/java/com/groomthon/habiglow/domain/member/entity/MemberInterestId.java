package com.groomthon.habiglow.domain.member.entity;

import java.io.Serializable;
import java.util.Objects;

import com.groomthon.habiglow.domain.routine.common.RoutineCategory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberInterestId implements Serializable {
	
	private Long member;
	private RoutineCategory category;
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberInterestId that = (MemberInterestId) o;
		return Objects.equals(member, that.member) && category == that.category;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(member, category);
	}
}