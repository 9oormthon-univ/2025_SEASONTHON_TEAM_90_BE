package com.groomthon.habiglow.domain.daily.entity;

import java.time.LocalDate;

import com.groomthon.habiglow.domain.member.entity.MemberEntity;
import com.groomthon.habiglow.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_reflection_table",
       indexes = {
           @Index(name = "idx_reflection_member_date", columnList = "member_id, reflection_date")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_member_reflection_date", columnNames = {"member_id", "reflection_date"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DailyReflectionEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reflection_id")
    private Long reflectionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;
    
    @Column(name = "reflection_content", columnDefinition = "TEXT")
    private String reflectionContent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", nullable = false)
    private EmotionType emotion;
    
    @Column(name = "reflection_date", nullable = false)
    private LocalDate reflectionDate;
    
    public void updateReflection(String content, EmotionType emotion) {
        this.reflectionContent = content;
        this.emotion = emotion;
    }
    
    public static DailyReflectionEntity create(MemberEntity member, String content, EmotionType emotion, LocalDate date) {
        return DailyReflectionEntity.builder()
                .member(member)
                .reflectionContent(content)
                .emotion(emotion)
                .reflectionDate(date)
                .build();
    }
}