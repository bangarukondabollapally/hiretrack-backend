package com.hiretrack.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachmentDto {
    private String type; // "text" or "image"
    private String name;
    
    // For text attachments
    private String content;
    
    // For image attachments
    private String mimeType;
    private String data; // base64
}
