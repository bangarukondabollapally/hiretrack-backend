package com.hiretrack.application;

import com.hiretrack.application.dto.ApplicationListDto;
import com.hiretrack.application.dto.ApplicationRequestDto;
import com.hiretrack.application.dto.ApplicationResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponseDto> createApplication(
            @Valid @RequestBody ApplicationRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ApplicationResponseDto response = applicationService.createApplication(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ApplicationListDto>> getUserApplications(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<ApplicationListDto> response = applicationService.getUserApplications(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ApplicationResponseDto response = applicationService.getApplicationById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ApplicationResponseDto response = applicationService.updateApplication(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        applicationService.deleteApplication(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
