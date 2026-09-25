package com.hiretrack.application;

import com.hiretrack.application.dto.ApplicationListDto;
import com.hiretrack.application.dto.ApplicationRequestDto;
import com.hiretrack.application.dto.ApplicationResponseDto;
import com.hiretrack.common.exception.ForbiddenException;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.interview.dto.InterviewResponseDto;
import com.hiretrack.tag.Tag;
import com.hiretrack.tag.dto.TagResponseDto;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplicationResponseDto createApplication(ApplicationRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = Application.builder()
                .companyName(request.getCompanyName())
                .jobRole(request.getJobRole())
                .status(request.getStatus())
                .jobType(request.getJobType())
                .jobUrl(request.getJobUrl())
                .notes(request.getNotes())
                .appliedDate(request.getAppliedDate())
                .followUpDate(request.getFollowUpDate())
                .jobDescription(request.getJobDescription())
                .user(user)
                .build();

        Application saved = applicationRepository.save(application);
        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationListDto> getUserApplications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return applicationRepository.findByUserIdOrderByAppliedDateDescIdDesc(user.getId())
                .stream()
                .map(this::mapToListDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationResponseDto getApplicationById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to view this application");
        }

        return mapToResponseDto(application);
    }

    @Transactional
    public ApplicationResponseDto updateApplication(Long id, ApplicationRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to update this application");
        }

        application.setCompanyName(request.getCompanyName());
        application.setJobRole(request.getJobRole());
        application.setStatus(request.getStatus());
        application.setJobType(request.getJobType());
        application.setJobUrl(request.getJobUrl());
        application.setNotes(request.getNotes());
        application.setAppliedDate(request.getAppliedDate());
        application.setFollowUpDate(request.getFollowUpDate());
        if (request.getJobDescription() != null) {
            application.setJobDescription(request.getJobDescription());
        }

        Application updated = applicationRepository.save(application);
        return mapToResponseDto(updated);
    }

    @Transactional
    public void deleteApplication(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to delete this application");
        }

        applicationRepository.delete(application);
    }

    public ApplicationListDto mapToListDto(Application application) {
        return ApplicationListDto.builder()
                .id(application.getId())
                .companyName(application.getCompanyName())
                .jobRole(application.getJobRole())
                .status(application.getStatus())
                .appliedDate(application.getAppliedDate())
                .followUpDate(application.getFollowUpDate())
                .tags(application.getTags().stream().map(Tag::getName).sorted().collect(Collectors.toList()))
                .build();
    }

    public ApplicationResponseDto mapToResponseDto(Application application) {
        List<TagResponseDto> tagDtos = application.getTags().stream()
                .map(t -> TagResponseDto.builder().id(t.getId()).name(t.getName()).build())
                .collect(Collectors.toList());

        List<InterviewResponseDto> interviewDtos = application.getInterviews().stream()
                .map(i -> InterviewResponseDto.builder()
                        .id(i.getId())
                        .applicationId(application.getId())
                        .round(i.getRound())
                        .interviewDate(i.getInterviewDate())
                        .interviewType(i.getInterviewType())
                        .outcome(i.getOutcome())
                        .notes(i.getNotes())
                        .build())
                .collect(Collectors.toList());

        return ApplicationResponseDto.builder()
                .id(application.getId())
                .userId(application.getUser().getId())
                .companyName(application.getCompanyName())
                .jobRole(application.getJobRole())
                .status(application.getStatus())
                .jobType(application.getJobType())
                .jobUrl(application.getJobUrl())
                .notes(application.getNotes())
                .jobDescription(application.getJobDescription())
                .appliedDate(application.getAppliedDate())
                .followUpDate(application.getFollowUpDate())
                .tags(tagDtos)
                .interviews(interviewDtos)
                .build();
    }
}
