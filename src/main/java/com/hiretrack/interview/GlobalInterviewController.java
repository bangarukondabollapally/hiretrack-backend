package com.hiretrack.interview;

import com.hiretrack.interview.dto.InterviewResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class GlobalInterviewController {

    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<List<InterviewResponseDto>> getAllInterviews(
            @RequestParam(required = false, defaultValue = "all") String scope,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) InterviewOutcome outcome,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<InterviewResponseDto> response = interviewService.getAllInterviews(scope, applicationId, outcome, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
