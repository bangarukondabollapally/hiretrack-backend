package com.hiretrack.opening.dto;

import com.hiretrack.opening.OpeningStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementOpeningRequestDto {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Job role is required")
    private String jobRole;

    private String jobType;

    private String location;

    private String workMode;

    private String packageDetails;

    private String eligibility;

    private String degree;

    private String eligibleBranches;

    private Integer graduationYearStart;

    private Integer graduationYearEnd;

    private String yearOfStudy;

    @jakarta.validation.constraints.Min(value = 1, message = "Seats must be a positive number")
    @jakarta.validation.constraints.Max(value = 10000, message = "Seats cannot exceed 10000")
    private Integer seats;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "Minimum CGPA cannot be negative")
    @jakarta.validation.constraints.DecimalMax(value = "10.0", message = "Minimum CGPA cannot exceed 10.0")
    private Double minCgpa;

    @jakarta.validation.constraints.Min(value = 0, message = "Maximum backlogs cannot be negative")
    private Integer maxBacklogs;

    private String eligibilityNote;

    private LocalDate deadline;

    @jakarta.validation.constraints.Size(max = 2000, message = "Description / Mini JD must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Application link is required")
    private String applicationLink;

    private OpeningStatus status;
}
