package com.hiretrack.dashboard.dto;

import com.hiretrack.application.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDto {

    private Map<ApplicationStatus, Long> statusCounts;
    private List<UpcomingInterviewDto> upcomingInterviews;
    private List<FollowUpDueDto> followUpsDue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingInterviewDto {
        private Long applicationId;
        private String companyName;
        private LocalDateTime interviewDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FollowUpDueDto {
        private Long applicationId;
        private String companyName;
        private LocalDate followUpDate;
    }
}
