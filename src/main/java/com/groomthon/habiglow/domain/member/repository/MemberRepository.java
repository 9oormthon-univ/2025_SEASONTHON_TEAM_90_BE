package com.groomthon.habiglow.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.global.oauth2.entity.SocialType;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

	Optional<MemberEntity> findByMemberEmail(String memberEmail);

	Optional<MemberEntity> findBySocialUniqueId(String socialUniqueId);

	Optional<MemberEntity> findBySocialUniqueIdAndSocialType(String socialUniqueId, SocialType socialType);

	Boolean existsBySocialUniqueId(String socialUniqueId);
}