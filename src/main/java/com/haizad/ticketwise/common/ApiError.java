package com.haizad.ticketwise.common;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		int status,
		String error,
		String message,
		String path,
		Instant timestamp,
		List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ApiError of(HttpStatusCode status, String message, String path) {
		return of(status, message, path, null);
	}

	public static ApiError of(HttpStatusCode status, String message, String path, List<FieldError> fieldErrors) {
		String reason = HttpStatus.valueOf(status.value()).getReasonPhrase();
		return new ApiError(status.value(), reason, message, path, Instant.now(), fieldErrors);
	}

}
