package com.hiretrack.dashboard;

import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.application.ApplicationStatus;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.dashboard.dto.DashboardResponseDto;
import com.hiretrack.interview.Interview;
import com.hiretrack.interview.InterviewRepository;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "dashboardCache", key = "#userEmail")
    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long userId = user.getId();
        LocalDate today = LocalDate.now();

        // 1. Status Counts
        Map<ApplicationStatus, Long> statusCounts = new EnumMap<>(ApplicationStatus.class);
        Arrays.stream(ApplicationStatus.values()).forEach(s -> statusCounts.put(s, 0L));

        List<Object[]> counts = applicationRepository.countByStatusGrouped(userId);
        for (Object[] row : counts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            Long count = (Long) row[1];
            statusCounts.put(status, count);
        }

        // 2. Upcoming Interviews (ALL future interviews, ascending, limit 10)
        LocalDateTime now = LocalDateTime.now();
        List<Interview> interviews = interviewRepository.findUpcomingInterviews(userId, now, org.springframework.data.domain.PageRequest.of(0, 10));

        List<DashboardResponseDto.UpcomingInterviewDto> upcoming = interviews.stream()
                .map(i -> DashboardResponseDto.UpcomingInterviewDto.builder()
                        .applicationId(i.getApplication().getId())
                        .companyName(i.getApplication().getCompanyName())
                        .jobRole(i.getApplication().getJobRole())
                        .interviewDate(i.getInterviewDate())
                        .build())
                .collect(Collectors.toList());

        // 3. Follow Ups Due (followUpDate <= today and status NOT IN (OFFER, REJECTED, WITHDRAWN))
        List<Application> followUps = applicationRepository.findFollowUpsDue(userId, today);

        List<DashboardResponseDto.FollowUpDueDto> followUpDtos = followUps.stream()
                .map(a -> DashboardResponseDto.FollowUpDueDto.builder()
                        .applicationId(a.getId())
                        .companyName(a.getCompanyName())
                        .followUpDate(a.getFollowUpDate())
                        .build())
                .collect(Collectors.toList());

        // 4. Upcoming Follow Ups (followUpDate > today and status NOT IN (OFFER, REJECTED, WITHDRAWN), limit 5)
        List<Application> upcomingFollowUps = applicationRepository.findUpcomingFollowUps(userId, today, org.springframework.data.domain.PageRequest.of(0, 5));

        List<DashboardResponseDto.FollowUpDueDto> upcomingFollowUpDtos = upcomingFollowUps.stream()
                .map(a -> DashboardResponseDto.FollowUpDueDto.builder()
                        .applicationId(a.getId())
                        .companyName(a.getCompanyName())
                        .followUpDate(a.getFollowUpDate())
                        .build())
                .collect(Collectors.toList());

        return DashboardResponseDto.builder()
                .statusCounts(statusCounts)
                .upcomingInterviews(upcoming)
                .followUpsDue(followUpDtos)
                .upcomingFollowUps(upcomingFollowUpDtos)
                .build();
    }
}
