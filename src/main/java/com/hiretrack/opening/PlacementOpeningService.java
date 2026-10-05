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

    @Transactional
    public PlacementOpeningResponseDto createOpening(PlacementOpeningRequestDto dto) {
        PlacementOpening opening = PlacementOpening.builder()
                .companyName(dto.getCompanyName())
                .jobRole(dto.getJobRole())
                .jobType(dto.getJobType())
                .location(dto.getLocation())
                .workMode(dto.getWorkMode())
                .packageDetails(dto.getPackageDetails())
                .eligibility(dto.getEligibility())
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
        PlacementOpening opening = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement opening not found with id: " + id));

        opening.setCompanyName(dto.getCompanyName());
        opening.setJobRole(dto.getJobRole());
        opening.setJobType(dto.getJobType());
        opening.setLocation(dto.getLocation());
        opening.setWorkMode(dto.getWorkMode());
        opening.setPackageDetails(dto.getPackageDetails());
        opening.setEligibility(dto.getEligibility());
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
                .deadline(entity.getDeadline())
                .description(entity.getDescription())
                .applicationLink(entity.getApplicationLink())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
