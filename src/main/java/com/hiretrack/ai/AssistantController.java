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
import com.hiretrack.ai.dto.ChatAttachmentDto;
import java.util.List;
import java.util.Set;

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
        validateAttachments(request.getAttachments());
        ChatResponseDto response = assistantService.chat(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chat/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> chatStream(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        validateAttachments(request.getAttachments());
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = 
                assistantService.chatStream(request, userDetails.getUsername());
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    private void validateAttachments(List<ChatAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) return;
        if (attachments.size() > 5) {
            throw new IllegalArgumentException("Maximum 5 attachments allowed");
        }
        Set<String> allowedImageMimeTypes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
        for (ChatAttachmentDto att : attachments) {
            if ("text".equals(att.getType())) {
                if (att.getContent() == null || att.getContent().length() > 20000) {
                    throw new IllegalArgumentException("Text attachment content exceeds maximum size of 20,000 characters");
                }
            } else if ("image".equals(att.getType())) {
                if (!allowedImageMimeTypes.contains(att.getMimeType())) {
                    throw new IllegalArgumentException("Unsupported image type: " + att.getMimeType());
                }
                if (att.getData() == null) {
                    throw new IllegalArgumentException("Image data is required");
                }
                // Verify magic bytes of base64 data
                byte[] decodedBytes;
                try {
                    decodedBytes = java.util.Base64.getDecoder().decode(att.getData());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid base64 encoding in image data");
                }
                if (!isValidImageMagicBytes(decodedBytes, att.getMimeType())) {
                    throw new IllegalArgumentException("Image content does not match specified mime type or is invalid");
                }
            } else {
                throw new IllegalArgumentException("Unsupported attachment type: " + att.getType());
            }
        }
    }

    private boolean isValidImageMagicBytes(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length < 4) return false;
        
        if ("image/jpeg".equals(mimeType)) {
            return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        } else if ("image/png".equals(mimeType)) {
            return (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47;
        } else if ("image/webp".equals(mimeType)) {
            if (bytes.length < 12) return false;
            return (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46 &&
                   (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45 && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50;
        } else if ("image/gif".equals(mimeType)) {
            return (bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46;
        }
        return false;
    }
}
