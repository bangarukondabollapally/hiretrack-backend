package com.hiretrack.ai;

import com.hiretrack.ai.dto.ChatRequestDto;
import com.hiretrack.ai.dto.ChatResponseDto;
import com.hiretrack.common.AsyncAuditService;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final GroqClient groqClient;
    private final PromptBuilder promptBuilder;
    private final UserRepository userRepository;
    private final AsyncAuditService asyncAuditService;

    @Transactional(readOnly = true)
    public ChatResponseDto chat(ChatRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String systemPrompt = promptBuilder.buildSystemPrompt(user.getId(), request.getApplicationId());
        String reply = groqClient.generateResponse(systemPrompt, request.getMessage(), request.getAttachments());

        // Asynchronous audit logging (TASK-032)
        asyncAuditService.logAiInteractionAsync(userEmail, request.getApplicationId(), request.getMessage());

        return ChatResponseDto.builder()
                .reply(reply)
                .build();
    }

    @Transactional(readOnly = true)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStream(ChatRequestDto request, String userEmail) {
        // Resolve user & check ownership synchronously on caller thread before streaming starts
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String systemPrompt = promptBuilder.buildSystemPrompt(user.getId(), request.getApplicationId());

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = 
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(60_000L);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Event 1: status retrieving
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("status").data(java.util.Map.of("phase", "retrieving")));

                // Event 2: status thinking
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("status").data(java.util.Map.of("phase", "thinking")));

                final boolean[] startedGenerating = {false};

                groqClient.streamResponse(
                        systemPrompt,
                        request.getMessage(),
                        request.getAttachments(),
                        token -> {
                            try {
                                if (!startedGenerating[0]) {
                                    startedGenerating[0] = true;
                                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                            .name("status").data(java.util.Map.of("phase", "generating")));
                                }
                                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                        .name("token").data(java.util.Map.of("text", token)));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        reasoning -> {
                            try {
                                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                        .name("reasoning").data(java.util.Map.of("text", reasoning)));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

                // Event 3: done
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("done").data(java.util.Map.of()));
                emitter.complete();

                asyncAuditService.logAiInteractionAsync(userEmail, request.getApplicationId(), request.getMessage());
            } catch (Exception ex) {
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error").data(java.util.Map.of("status", 500, "message", ex.getMessage() != null ? ex.getMessage() : "Error during streaming")));
                    emitter.completeWithError(ex);
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }
}
