package com.hiretrack.application.dto;

import com.hiretrack.application.ApplicationStatus;
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
public class ApplicationListDto {
    private Long id;
    private String companyName;
    private String jobRole;
    private ApplicationStatus status;
    private LocalDate appliedDate;
    private LocalDate followUpDate;
    private List<String> tags;
}
