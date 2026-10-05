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

    private String eligibilityNote;

    private LocalDate deadline;

    private String description;

    @NotBlank(message = "Application link is required")
    @Pattern(regexp = "^https?://.*", message = "Application link must be a valid HTTP or HTTPS URL")
    private String applicationLink;

    private OpeningStatus status;
}
