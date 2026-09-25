package com.hiretrack.application.dto;

import com.hiretrack.application.ApplicationStatus;
import com.hiretrack.interview.dto.InterviewResponseDto;
import com.hiretrack.tag.dto.TagResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDto {
    private Long id;
    private Long userId;
    private String companyName;
    private String jobRole;
    private ApplicationStatus status;
    private String jobType;
    private String jobUrl;
    private String notes;
    private String jobDescription;
    private LocalDate appliedDate;
    private LocalDate followUpDate;
    private List<TagResponseDto> tags;
    private List<InterviewResponseDto> interviews;
}
