package com.hiretrack.opening.dto;

import com.hiretrack.opening.OpeningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementOpeningResponseDto {
    private Long id;
    private String companyName;
    private String jobRole;
    private String jobType;
    private String location;
    private String workMode;
    private String packageDetails;
    private String eligibility;
    private LocalDate deadline;
    private String description;
    private String applicationLink;
    private OpeningStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
