package com.hiretrack.opening;

import com.hiretrack.opening.dto.PlacementOpeningRequestDto;
import com.hiretrack.opening.dto.PlacementOpeningResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/openings")
@RequiredArgsConstructor
public class AdminPlacementOpeningController {

    private final PlacementOpeningService service;

    @GetMapping
    public ResponseEntity<List<PlacementOpeningResponseDto>> getAllOpenings() {
        return ResponseEntity.ok(service.getOpenings(true));
    }

    @PostMapping
    public ResponseEntity<PlacementOpeningResponseDto> createOpening(
            @Valid @RequestBody PlacementOpeningRequestDto dto
    ) {
        PlacementOpeningResponseDto created = service.createOpening(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlacementOpeningResponseDto> updateOpening(
            @PathVariable Long id,
            @Valid @RequestBody PlacementOpeningRequestDto dto
    ) {
        return ResponseEntity.ok(service.updateOpening(id, dto));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<PlacementOpeningResponseDto> closeOpening(@PathVariable Long id) {
        return ResponseEntity.ok(service.closeOpening(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpening(@PathVariable Long id) {
        service.deleteOpening(id);
        return ResponseEntity.noContent().build();
    }
}
