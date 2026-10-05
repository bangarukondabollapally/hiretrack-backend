package com.hiretrack.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDto {

    @NotBlank(message = "Message is required")
    private String message;

    private Long applicationId;

    private Long conversationId;

    private List<ChatAttachmentDto> attachments;
}
