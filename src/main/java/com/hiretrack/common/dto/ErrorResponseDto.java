package com.hiretrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard error response shape per docs/API.md:
 * { "status": 400, "message": "...", "timestamp": "..." }
 */
@Getter
@AllArgsConstructor
public class ErrorResponseDto {
    private int status;
    private String message;
    private String timestamp;

    public static ErrorResponseDto of(int status, String message) {
        return new ErrorResponseDto(status, message, Instant.now().toString());
    }
}
