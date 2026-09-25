package com.hiretrack.interview;

import com.hiretrack.interview.dto.InterviewRequestDto;
import com.hiretrack.interview.dto.InterviewResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<InterviewResponseDto> createInterview(
            @PathVariable Long applicationId,
            @Valid @RequestBody InterviewRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        InterviewResponseDto response = interviewService.createInterview(applicationId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InterviewResponseDto>> getInterviews(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<InterviewResponseDto> response = interviewService.getInterviewsByApplication(applicationId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponseDto> updateInterview(
            @PathVariable Long applicationId,
            @PathVariable Long id,
            @Valid @RequestBody InterviewRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        InterviewResponseDto response = interviewService.updateInterview(applicationId, id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable Long applicationId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        interviewService.deleteInterview(applicationId, id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
