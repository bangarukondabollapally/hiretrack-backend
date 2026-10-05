package com.hiretrack.assistant.dto;

import com.hiretrack.assistant.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDto {
    private Long id;
    private Long conversationId;
    private MessageRole role;
    private String content;
    private LocalDateTime createdAt;
}
