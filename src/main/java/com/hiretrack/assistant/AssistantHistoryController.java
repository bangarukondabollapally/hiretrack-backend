package com.hiretrack.assistant;

import com.hiretrack.assistant.dto.ChatMessageResponseDto;
import com.hiretrack.assistant.dto.ConversationRequestDto;
import com.hiretrack.assistant.dto.ConversationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assistant/conversations")
@RequiredArgsConstructor
public class AssistantHistoryController {

    private final AssistantHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<ConversationResponseDto>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(historyService.getConversations(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ConversationResponseDto> createConversation(
            @RequestBody(required = false) ConversationRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historyService.createConversation(dto, userDetails.getUsername()));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageResponseDto>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(historyService.getMessages(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversationResponseDto> renameConversation(
            @PathVariable Long id,
            @RequestBody ConversationRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(historyService.renameConversation(id, dto, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        historyService.deleteConversation(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
