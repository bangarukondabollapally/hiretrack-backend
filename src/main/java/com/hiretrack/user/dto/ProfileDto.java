package com.hiretrack.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDto {
    private String name;
    private String resumeText;
    private String targetRole;

    @Min(value = 0, message = "yearsOfExperience must be at least 0")
    @Max(value = 60, message = "yearsOfExperience must be at most 60")
    private Integer yearsOfExperience;

    @Size(max = 3000, message = "experienceSummary must not exceed 3000 characters")
    private String experienceSummary;

    private String avatarPreset;
}
