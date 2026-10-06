package com.hiretrack.opening;

import com.hiretrack.opening.dto.PlacementOpeningResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/openings")
@RequiredArgsConstructor
public class PlacementOpeningController {

    private final PlacementOpeningService service;

    @GetMapping
    public ResponseEntity<List<PlacementOpeningResponseDto>> getOpenings(
            @RequestParam(defaultValue = "false") boolean includeClosed,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(service.getOpenings(includeClosed, email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlacementOpeningResponseDto> getOpeningById(
            @PathVariable Long id,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(service.getOpeningById(id, email));
    }

    @PostMapping("/{id}/track")
    public ResponseEntity<Map<String, Object>> trackOpening(
            @PathVariable Long id,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(service.trackOpening(id, email));
    }

    @DeleteMapping("/{id}/track")
    public ResponseEntity<Map<String, Object>> untrackOpening(
            @PathVariable Long id,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(service.untrackOpening(id, email));
    }
}
