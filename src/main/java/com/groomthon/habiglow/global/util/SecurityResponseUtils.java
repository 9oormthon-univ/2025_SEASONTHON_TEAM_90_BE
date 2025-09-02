package com.groomthon.habiglow.global.util;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groomthon.habiglow.global.dto.CommonApiResponse;
import com.groomthon.habiglow.global.response.ErrorCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityResponseUtils {
	
	private final ObjectMapper objectMapper;
	
	public void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		
		CommonApiResponse<Void> errorResponse = CommonApiResponse.fail(errorCode);
		String jsonResponse = objectMapper.writeValueAsString(errorResponse);
		
		response.getWriter().write(jsonResponse);
		response.getWriter().flush();
		
		log.debug("Security error response sent: code={}, message={}", errorCode.getCode(), errorCode.getMessage());
	}
}