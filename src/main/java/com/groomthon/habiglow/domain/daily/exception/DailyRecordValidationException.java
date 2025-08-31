package com.groomthon.habiglow.domain.daily.exception;

import java.util.List;

import com.groomthon.habiglow.domain.daily.dto.error.InvalidRoutineError;
import com.groomthon.habiglow.global.exception.BaseException;
import com.groomthon.habiglow.global.response.ErrorCode;

import lombok.Getter;

@Getter
public class DailyRecordValidationException extends BaseException {
    
    private final List<InvalidRoutineError> invalidRoutines;
    
    public DailyRecordValidationException(List<InvalidRoutineError> invalidRoutines) {
        super(ErrorCode.DAILY_RECORD_INVALID_ROUTINES);
        this.invalidRoutines = invalidRoutines;
    }
}