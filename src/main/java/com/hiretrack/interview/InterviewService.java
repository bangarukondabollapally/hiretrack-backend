package com.hiretrack.interview;

import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.common.exception.ForbiddenException;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.interview.dto.InterviewRequestDto;
import com.hiretrack.interview.dto.InterviewResponseDto;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public InterviewResponseDto createInterview(Long applicationId, InterviewRequestDto request, String userEmail) {
        Application application = getOwnedApplication(applicationId, userEmail);

        Interview interview = Interview.builder()
                .round(request.getRound())
                .interviewDate(request.getInterviewDate())
                .interviewType(request.getInterviewType())
                .outcome(request.getOutcome() != null ? request.getOutcome() : InterviewOutcome.PENDING)
                .notes(request.getNotes())
                .application(application)
                .build();

        Interview saved = interviewRepository.save(interview);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponseDto> getInterviewsByApplication(Long applicationId, String userEmail) {
        getOwnedApplication(applicationId, userEmail);

        return interviewRepository.findByApplicationIdOrderByInterviewDateAsc(applicationId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewResponseDto updateInterview(Long applicationId, Long interviewId, InterviewRequestDto request, String userEmail) {
        getOwnedApplication(applicationId, userEmail);

        Interview interview = interviewRepository.findByIdAndApplicationId(interviewId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId + " for application: " + applicationId));

        interview.setRound(request.getRound());
        interview.setInterviewDate(request.getInterviewDate());
        interview.setInterviewType(request.getInterviewType());
        if (request.getOutcome() != null) {
            interview.setOutcome(request.getOutcome());
        }
        interview.setNotes(request.getNotes());

        Interview updated = interviewRepository.save(interview);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteInterview(Long applicationId, Long interviewId, String userEmail) {
        getOwnedApplication(applicationId, userEmail);

        Interview interview = interviewRepository.findByIdAndApplicationId(interviewId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId + " for application: " + applicationId));

        interviewRepository.delete(interview);
    }

    private Application getOwnedApplication(Long applicationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to access interviews for this application");
        }

        return application;
    }

    public InterviewResponseDto mapToDto(Interview interview) {
        return InterviewResponseDto.builder()
                .id(interview.getId())
                .applicationId(interview.getApplication().getId())
                .round(interview.getRound())
                .interviewDate(interview.getInterviewDate())
                .interviewType(interview.getInterviewType())
                .outcome(interview.getOutcome())
                .notes(interview.getNotes())
                .build();
    }
}
