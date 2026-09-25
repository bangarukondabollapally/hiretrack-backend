package com.hiretrack.interview.dto;

import com.hiretrack.interview.InterviewOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewRequestDto {

    @NotBlank(message = "Round is required")
    private String round;

    @NotNull(message = "Interview date is required")
    private LocalDateTime interviewDate;

    private String interviewType;

    private InterviewOutcome outcome;

    private String notes;
}
