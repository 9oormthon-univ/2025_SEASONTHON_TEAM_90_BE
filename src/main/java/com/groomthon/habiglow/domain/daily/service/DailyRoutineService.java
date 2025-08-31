package com.groomthon.habiglow.domain.daily.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groomthon.habiglow.domain.daily.dto.request.RoutinePerformanceRequest;
import com.groomthon.habiglow.domain.daily.entity.DailyRoutineEntity;
import com.groomthon.habiglow.domain.daily.repository.DailyRoutineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyRoutineService {

    private final DailyRoutineRepository dailyRoutineRepository;
    private final ConsecutiveDaysCalculator consecutiveDaysCalculator;

    public List<DailyRoutineEntity> saveRoutineRecords(Long memberId, LocalDate date,
                                                       List<RoutinePerformanceRequest> records) {

        dailyRoutineRepository.deleteByMemberIdAndPerformedDate(memberId, date);

        List<DailyRoutineEntity> entities = new ArrayList<>();

        for (RoutinePerformanceRequest record : records) {
            int consecutiveDays = consecutiveDaysCalculator.calculate(
                    record.getRoutineId(), memberId, date, record.getPerformanceLevel());

            DailyRoutineEntity entity = DailyRoutineEntity.create(
                    record.getRoutine(),
                    record.getMember(),
                    record.getPerformanceLevel(),
                    date,
                    consecutiveDays
            );

            entities.add(entity);
        }

        return dailyRoutineRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<DailyRoutineEntity> getTodayRoutines(Long memberId, LocalDate date) {
        return dailyRoutineRepository.findByMemberIdAndPerformedDateWithRoutine(memberId, date);
    }
}