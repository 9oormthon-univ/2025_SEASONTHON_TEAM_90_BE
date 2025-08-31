package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.entity.DailyReflectionEntity;
import com.groomthon.habiglow.domain.daily.entity.EmotionType;
import com.groomthon.habiglow.domain.daily.repository.DailyReflectionRepository;
import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.domain.member.repository.MemberRepository;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReflectionService {
    
    private final DailyReflectionRepository reflectionRepository;
    private final MemberRepository memberRepository;
    
    public DailyReflectionEntity saveReflection(Long memberId, LocalDate date, 
                                               String content, EmotionType emotion) {
        
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
        
        Optional<DailyReflectionEntity> existing = reflectionRepository
            .findByMemberIdAndReflectionDate(memberId, date);
        
        if (existing.isPresent()) {
            DailyReflectionEntity entity = existing.get();
            entity.updateReflection(content, emotion);
            return entity;
        }
        
        DailyReflectionEntity entity = DailyReflectionEntity.create(member, content, emotion, date);
        return reflectionRepository.save(entity);
    }
    
    @Transactional(readOnly = true)
    public Optional<DailyReflectionEntity> getReflection(Long memberId, LocalDate date) {
        return reflectionRepository.findByMemberIdAndReflectionDate(memberId, date);
    }
}