package com.hiretrack.opening;

import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.opening.dto.PlacementOpeningRequestDto;
import com.hiretrack.opening.dto.PlacementOpeningResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlacementOpeningService {

    private final PlacementOpeningRepository repository;

    @Transactional(readOnly = true)
    public List<PlacementOpeningResponseDto> getOpenings(boolean includeClosed) {
        List<PlacementOpening> openings = includeClosed
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findOpenAndActiveOpenings(java.time.LocalDate.now());

        return openings.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlacementOpeningResponseDto getOpeningById(Long id) {
        PlacementOpening opening = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + id));
        return mapToResponseDto(opening);
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
        if (dto.getGraduationYearStart() != null || dto.getGraduationYearEnd() != null) {
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
        if (dto.getEligibilityNote() != null && !dto.getEligibilityNote().isBlank()) {
            if (sb.length() > 0) sb.append(" (").append(dto.getEligibilityNote()).append(")");
            else sb.append(dto.getEligibilityNote());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @Transactional
    public PlacementOpeningResponseDto createOpening(PlacementOpeningRequestDto dto) {
        validateGraduationYears(dto.getGraduationYearStart(), dto.getGraduationYearEnd());

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
                .eligibilityNote(dto.getEligibilityNote())
                .deadline(dto.getDeadline())
                .description(dto.getDescription())
                .applicationLink(dto.getApplicationLink())
                .status(dto.getStatus() != null ? dto.getStatus() : OpeningStatus.OPEN)
                .build();

        PlacementOpening saved = repository.save(opening);
        return mapToResponseDto(saved);
    }

    @Transactional
    public PlacementOpeningResponseDto updateOpening(Long id, PlacementOpeningRequestDto dto) {
        validateGraduationYears(dto.getGraduationYearStart(), dto.getGraduationYearEnd());

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
        opening.setEligibilityNote(dto.getEligibilityNote());
        opening.setDeadline(dto.getDeadline());
        opening.setDescription(dto.getDescription());
        opening.setApplicationLink(dto.getApplicationLink());
        if (dto.getStatus() != null) {
            opening.setStatus(dto.getStatus());
        }

        PlacementOpening updated = repository.save(opening);
        return mapToResponseDto(updated);
    }

    @Transactional
    public PlacementOpeningResponseDto closeOpening(Long id) {
        PlacementOpening opening = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + id));

        opening.setStatus(OpeningStatus.CLOSED);
        PlacementOpening updated = repository.save(opening);
        return mapToResponseDto(updated);
    }

    @Transactional
    public void deleteOpening(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Placement opening not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public PlacementOpeningResponseDto mapToResponseDto(PlacementOpening entity) {
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
                .eligibilityNote(entity.getEligibilityNote())
                .publishedBy("Placement Cell")
                .deadline(entity.getDeadline())
                .description(entity.getDescription())
                .applicationLink(entity.getApplicationLink())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
