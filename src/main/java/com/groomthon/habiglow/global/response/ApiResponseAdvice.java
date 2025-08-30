package com.groomthon.habiglow.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.groomthon.habiglow.global.dto.CommonApiResponse;

/**
 * API 응답을 자동으로 CommonApiResponse로 감싸주는 Advice
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.hasMethodAnnotation(AutoApiResponse.class)
			|| returnType.getDeclaringClass().isAnnotationPresent(AutoApiResponse.class);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType,
		ServerHttpRequest request, ServerHttpResponse response) {

		if (body instanceof CommonApiResponse) {
			return body;
		}

		// @SuccessCode 어노테이션이 있으면 지정된 성공 코드 사용
		SuccessCode successCodeAnnotation = returnType.getMethodAnnotation(SuccessCode.class);
<<<<<<< HEAD
		ApiSuccessCode successCode = successCodeAnnotation != null
			? successCodeAnnotation.value()
			: ApiSuccessCode.SUCCESS;
=======
		MemberSuccessCode successCode = successCodeAnnotation != null
			? successCodeAnnotation.value()
			: MemberSuccessCode.SUCCESS;
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd

		if (body == null || returnType.getParameterType() == Void.class) {
			return CommonApiResponse.success(successCode);
		}

		return CommonApiResponse.success(successCode, body);
	}
<<<<<<< HEAD
}
=======
}
>>>>>>> 803266e19157d4b2789d0538cff2c8d75a3a0abd
