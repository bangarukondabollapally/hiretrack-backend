package com.hiretrack.application.dto;

import com.hiretrack.application.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequestDto {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Job role is required")
    private String jobRole;

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String jobType;

    private String jobUrl;

    private String notes;

    private LocalDate appliedDate;

    private LocalDate followUpDate;

    private String jobDescription;
}
