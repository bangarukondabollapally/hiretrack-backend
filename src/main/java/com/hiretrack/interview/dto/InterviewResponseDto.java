package com.hiretrack.interview.dto;

import com.hiretrack.interview.InterviewOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponseDto {
    private Long id;
    private Long applicationId;
    private String round;
    private LocalDateTime interviewDate;
    private String interviewType;
    private InterviewOutcome outcome;
    private String notes;
}
