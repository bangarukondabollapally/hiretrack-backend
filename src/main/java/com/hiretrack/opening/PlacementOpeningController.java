package com.hiretrack.opening;

import com.hiretrack.opening.dto.PlacementOpeningResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/openings")
@RequiredArgsConstructor
public class PlacementOpeningController {

    private final PlacementOpeningService service;

    @GetMapping
    public ResponseEntity<List<PlacementOpeningResponseDto>> getOpenings(
            @RequestParam(defaultValue = "false") boolean includeClosed
    ) {
        return ResponseEntity.ok(service.getOpenings(includeClosed));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlacementOpeningResponseDto> getOpeningById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOpeningById(id));
    }
}
