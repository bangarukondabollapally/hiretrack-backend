package com.hiretrack.ai;

import com.hiretrack.ai.dto.ChatRequestDto;
import com.hiretrack.ai.dto.ChatResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chat(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ChatResponseDto response = assistantService.chat(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
