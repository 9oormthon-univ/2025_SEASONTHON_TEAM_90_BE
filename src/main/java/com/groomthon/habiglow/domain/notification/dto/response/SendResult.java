package com.groomthon.habiglow.domain.notification.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SendResult {
    private final int success;
    private final int failure;
}