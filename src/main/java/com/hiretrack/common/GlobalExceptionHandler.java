package com.hiretrack.common;

import com.hiretrack.common.dto.ErrorResponseDto;
import com.hiretrack.common.exception.EmailAlreadyExistsException;
import com.hiretrack.common.exception.ForbiddenException;
import com.hiretrack.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — centralised error handling for all controllers.
 *
 * Error response shape per docs/API.md:
 * { "status": 400, "message": "...", "timestamp": "..." }
 *
 * No stack traces are ever exposed in responses (docs/SECURITY.md).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400 — Bean Validation failure (@Valid on request DTOs) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ErrorResponseDto.of(400, message));
    }

    /** 401 — Wrong credentials at login */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.of(401, "Invalid email or password"));
    }

    /** 403 — Ownership check failed */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDto> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.of(403, ex.getMessage()));
    }

    /** 404 — Resource not found */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(404, ex.getMessage()));
    }

    /** 409 — Duplicate email on registration */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailConflict(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.of(409, ex.getMessage()));
    }

    @ExceptionHandler(com.hiretrack.common.exception.AiServiceException.class)
    public ResponseEntity<ErrorResponseDto> handleAiServiceException(com.hiretrack.common.exception.AiServiceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponseDto.of(502, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(500, "An unexpected error occurred. Please try again."));
    }
}
