package com.hiretrack.opening;

import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.opening.dto.PlacementOpeningRequestDto;
import com.hiretrack.opening.dto.PlacementOpeningResponseDto;
import com.hiretrack.user.Profile;
import com.hiretrack.user.ProfileRepository;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlacementOpeningService {

    private final PlacementOpeningRepository repository;
    private final UserTrackedOpeningRepository trackedOpeningRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<PlacementOpeningResponseDto> getOpenings(boolean includeClosed, String userEmail) {
        User currentUser = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        Long userId = currentUser != null ? currentUser.getId() : null;

        List<PlacementOpening> openings = includeClosed
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findOpenAndActiveOpenings(LocalDate.now());

        Set<Long> trackedOpeningIds = userId != null
                ? trackedOpeningRepository.findTrackedOpeningIdsByUserId(userId)
                : Collections.emptySet();

        return openings.stream()
                .map(op -> mapToResponseDto(op, trackedOpeningIds))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlacementOpeningResponseDto getOpeningById(Long id, String userEmail) {
        PlacementOpening opening = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + id));

        User currentUser = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        Long userId = currentUser != null ? currentUser.getId() : null;

        Set<Long> trackedOpeningIds = userId != null
                ? trackedOpeningRepository.findTrackedOpeningIdsByUserId(userId)
                : Collections.emptySet();

        return mapToResponseDto(opening, trackedOpeningIds);
    }

    @Transactional(readOnly = true)
    public List<PlacementOpeningResponseDto> getTrackedOpenings(String userEmail) {
        User currentUser = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        if (currentUser == null) {
            return Collections.emptyList();
        }

        List<PlacementOpening> trackedOpenings = trackedOpeningRepository.findTrackedOpeningsByUserId(currentUser.getId());
        Set<Long> trackedOpeningIds = trackedOpenings.stream()
                .map(PlacementOpening::getId)
                .collect(Collectors.toSet());

        return trackedOpenings.stream()
                .map(op -> mapToResponseDto(op, trackedOpeningIds))
                .collect(Collectors.toList());
    }

    private void validateGraduationYears(Integer start, Integer end) {
        if (start != null && (start < 2000 || start > 2100)) {
            throw new IllegalArgumentException("Graduation start year must be a valid year between 2000 and 2100");
        }
        if (end != null && (end < 2000 || end > 2100)) {
            throw new IllegalArgumentException("Graduation end year must be a valid year between 2000 and 2100");
        }
        if (start != null && end != null && start > end) {
            throw new IllegalArgumentException("Graduation start year cannot be greater than end year");
        }
    }

    private String normalizeAndValidateLink(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Application link is required");
        }
        String trimmed = rawUrl.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new IllegalArgumentException("Invalid application link scheme");
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            URI uri = new URI(trimmed);
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException("Application link must contain a valid domain host");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid application link format");
        }
        return trimmed;
    }

    private String resolveEligibilityText(PlacementOpeningRequestDto dto) {
        if (dto.getEligibility() != null && !dto.getEligibility().isBlank()) {
            return dto.getEligibility();
        }
        StringBuilder sb = new StringBuilder();
        if (dto.getDegree() != null && !dto.getDegree().isBlank()) {
            sb.append(dto.getDegree());
        }
        if (dto.getEligibleBranches() != null && !dto.getEligibleBranches().isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(dto.getEligibleBranches());
        }
        if (dto.getYearOfStudy() != null && !dto.getYearOfStudy().isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(dto.getYearOfStudy());
        } else if (dto.getGraduationYearStart() != null || dto.getGraduationYearEnd() != null) {
            if (sb.length() > 0) sb.append(" — ");
            if (dto.getGraduationYearStart() != null && dto.getGraduationYearEnd() != null) {
                if (dto.getGraduationYearStart().equals(dto.getGraduationYearEnd())) {
                    sb.append(dto.getGraduationYearStart());
                } else {
                    sb.append(dto.getGraduationYearStart()).append(" to ").append(dto.getGraduationYearEnd());
                }
            } else if (dto.getGraduationYearStart() != null) {
                sb.append(dto.getGraduationYearStart()).append("+");
            } else {
                sb.append("up to ").append(dto.getGraduationYearEnd());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @Transactional
    public PlacementOpeningResponseDto createOpening(PlacementOpeningRequestDto dto, String adminEmail) {
        validateGraduationYears(dto.getGraduationYearStart(), dto.getGraduationYearEnd());
        String normalizedLink = normalizeAndValidateLink(dto.getApplicationLink());

        if (dto.getDescription() != null && dto.getDescription().length() > 2000) {
            throw new IllegalArgumentException("Description / Mini JD must not exceed 2000 characters");
        }

        if (dto.getMinCgpa() != null && (dto.getMinCgpa() < 0.0 || dto.getMinCgpa() > 10.0)) {
            throw new IllegalArgumentException("Minimum CGPA must be between 0.0 and 10.0");
        }
        if (dto.getMaxBacklogs() != null && dto.getMaxBacklogs() < 0) {
            throw new IllegalArgumentException("Maximum backlogs cannot be negative");
        }

        User adminUser = adminEmail != null ? userRepository.findByEmail(adminEmail).orElse(null) : null;

        PlacementOpening opening = PlacementOpening.builder()
                .companyName(dto.getCompanyName())
                .jobRole(dto.getJobRole())
                .jobType(dto.getJobType())
                .location(dto.getLocation())
                .workMode(dto.getWorkMode())
                .packageDetails(dto.getPackageDetails())
                .eligibility(resolveEligibilityText(dto))
                .degree(dto.getDegree())
                .eligibleBranches(dto.getEligibleBranches())
                .graduationYearStart(dto.getGraduationYearStart())
                .graduationYearEnd(dto.getGraduationYearEnd())
                .yearOfStudy(dto.getYearOfStudy() != null && !dto.getYearOfStudy().isBlank() ? dto.getYearOfStudy() : "All Years")
                .minCgpa(dto.getMinCgpa())
                .maxBacklogs(dto.getMaxBacklogs())
                .deadline(dto.getDeadline())
                .description(dto.getDescription())
                .applicationLink(normalizedLink)
                .status(dto.getStatus() != null ? dto.getStatus() : OpeningStatus.OPEN)
                .createdBy(adminUser)
                .build();

        PlacementOpening saved = repository.save(opening);
        return mapToResponseDto(saved, Collections.emptySet());
    }

    @Transactional
    public PlacementOpeningResponseDto updateOpening(Long id, PlacementOpeningRequestDto dto, String adminEmail) {
        validateGraduationYears(dto.getGraduationYearStart(), dto.getGraduationYearEnd());
        String normalizedLink = normalizeAndValidateLink(dto.getApplicationLink());

        if (dto.getDescription() != null && dto.getDescription().length() > 2000) {
            throw new IllegalArgumentException("Description / Short JD must not exceed 2000 characters");
        }
        if (dto.getMinCgpa() != null && (dto.getMinCgpa() < 0.0 || dto.getMinCgpa() > 10.0)) {
            throw new IllegalArgumentException("Minimum CGPA must be between 0.0 and 10.0");
        }
        if (dto.getMaxBacklogs() != null && dto.getMaxBacklogs() < 0) {
            throw new IllegalArgumentException("Maximum backlogs cannot be negative");
        }

        PlacementOpening opening = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + id));

        opening.setCompanyName(dto.getCompanyName());
        opening.setJobRole(dto.getJobRole());
        opening.setJobType(dto.getJobType());
        opening.setLocation(dto.getLocation());
        opening.setWorkMode(dto.getWorkMode());
        opening.setPackageDetails(dto.getPackageDetails());
        opening.setEligibility(resolveEligibilityText(dto));
        opening.setDegree(dto.getDegree());
        opening.setEligibleBranches(dto.getEligibleBranches());
        opening.setGraduationYearStart(dto.getGraduationYearStart());
        opening.setGraduationYearEnd(dto.getGraduationYearEnd());
        if (dto.getYearOfStudy() != null) {
            opening.setYearOfStudy(dto.getYearOfStudy());
        }
        opening.setMinCgpa(dto.getMinCgpa());
        opening.setMaxBacklogs(dto.getMaxBacklogs());
        opening.setDeadline(dto.getDeadline());
        opening.setDescription(dto.getDescription());
        opening.setApplicationLink(normalizedLink);
        if (dto.getStatus() != null) {
            opening.setStatus(dto.getStatus());
        }

        PlacementOpening updated = repository.save(opening);
        return mapToResponseDto(updated, Collections.emptySet());
    }

    @Transactional
    public PlacementOpeningResponseDto closeOpening(Long id) {
        PlacementOpening opening = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + id));

        opening.setStatus(OpeningStatus.CLOSED);
        PlacementOpening updated = repository.save(opening);
        return mapToResponseDto(updated, Collections.emptySet());
    }

    @Transactional
    public void deleteOpening(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Placement opening not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> trackOpening(Long openingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        PlacementOpening opening = repository.findById(openingId)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + openingId));

        if (!trackedOpeningRepository.existsByUserIdAndPlacementOpeningId(user.getId(), openingId)) {
            UserTrackedOpening tracked = UserTrackedOpening.builder()
                    .user(user)
                    .placementOpening(opening)
                    .build();
            trackedOpeningRepository.save(tracked);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("openingId", openingId);
        response.put("isTracked", true);
        response.put("message", "Opening tracked successfully");
        return response;
    }

    @Transactional
    public Map<String, Object> untrackOpening(Long openingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!repository.existsById(openingId)) {
            throw new ResourceNotFoundException("Placement opening not found with id: " + openingId);
        }

        trackedOpeningRepository.deleteByUserIdAndPlacementOpeningId(user.getId(), openingId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("openingId", openingId);
        response.put("isTracked", false);
        response.put("message", "Opening untracked successfully");
        return response;
    }

    public PlacementOpeningResponseDto mapToResponseDto(PlacementOpening entity, Set<Long> trackedOpeningIds) {
        String publishedByStr = resolvePublishedBy(entity);
        boolean isTracked = trackedOpeningIds != null && trackedOpeningIds.contains(entity.getId());

        return PlacementOpeningResponseDto.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .jobRole(entity.getJobRole())
                .jobType(entity.getJobType())
                .location(entity.getLocation())
                .workMode(entity.getWorkMode())
                .packageDetails(entity.getPackageDetails())
                .eligibility(entity.getEligibility())
                .degree(entity.getDegree())
                .eligibleBranches(entity.getEligibleBranches())
                .graduationYearStart(entity.getGraduationYearStart())
                .graduationYearEnd(entity.getGraduationYearEnd())
                .yearOfStudy(entity.getYearOfStudy() != null ? entity.getYearOfStudy() : "All Years")
                .minCgpa(entity.getMinCgpa())
                .maxBacklogs(entity.getMaxBacklogs())
                .publishedBy(publishedByStr)
                .isTracked(isTracked)
                .deadline(entity.getDeadline())
                .description(entity.getDescription())
                .applicationLink(entity.getApplicationLink())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String resolvePublishedBy(PlacementOpening entity) {
        if (entity.getCreatedBy() != null) {
            Profile profile = profileRepository.findByUserId(entity.getCreatedBy().getId()).orElse(null);
            String name = (profile != null && profile.getName() != null && !profile.getName().trim().isEmpty())
                    ? profile.getName().trim()
                    : null;
            if (name != null) {
                return name;
            }
        }
        return "Placement Cell";
    }
}
