package com.haizad.ticketwise.common;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ResponseEntity<ApiError> handleInvalidTransition(InvalidStatusTransitionException ex,
			HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(EmailAlreadyUsedException.class)
	public ResponseEntity<ApiError> handleEmailAlreadyUsed(EmailAlreadyUsedException ex, HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<ApiError> handleServiceUnavailable(ServiceUnavailableException ex,
			HttpServletRequest request) {
		return respond(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleBadLogin(AuthenticationException ex, HttpServletRequest request) {
		return respond(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}

	@ExceptionHandler(PropertyReferenceException.class)
	public ResponseEntity<ApiError> handleInvalidSort(PropertyReferenceException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, "Cannot sort by '" + ex.getPropertyName() + "'", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		logger.error("Unexpected error on " + request.getRequestURI(), ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		ApiError body = ApiError.of(status, "Validation failed", pathOf(request), fieldErrors);
		return new ResponseEntity<>(body, headers, status);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String message = (body instanceof ProblemDetail problem) ? problem.getDetail() : ex.getMessage();
		return new ResponseEntity<>(ApiError.of(status, message, pathOf(request)), headers, status);
	}

	private static ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
	}

	private static String pathOf(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

}
