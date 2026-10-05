package com.hiretrack.ai;

import com.hiretrack.ai.dto.ChatRequestDto;
import com.hiretrack.ai.dto.ChatResponseDto;
import com.hiretrack.assistant.ChatMessage;
import com.hiretrack.assistant.ChatMessageRepository;
import com.hiretrack.assistant.Conversation;
import com.hiretrack.assistant.ConversationRepository;
import com.hiretrack.assistant.MessageRole;
import com.hiretrack.common.AsyncAuditService;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final GroqClient groqClient;
    private final PromptBuilder promptBuilder;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AsyncAuditService asyncAuditService;

    @Transactional
    public ChatResponseDto chat(ChatRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation = resolveOrCreateConversation(request, user);
        List<ChatMessage> history = loadRecentHistory(conversation.getId());

        // Save User Message
        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        String systemPrompt = promptBuilder.buildSystemPrompt(user.getId(), request.getApplicationId(), history);
        String reply = groqClient.generateResponse(systemPrompt, request.getMessage(), request.getAttachments());

        // Save Assistant Response Message
        ChatMessage assistantMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(reply)
                .build();
        chatMessageRepository.save(assistantMsg);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Asynchronous audit logging
        asyncAuditService.logAiInteractionAsync(userEmail, request.getApplicationId(), request.getMessage());

        return ChatResponseDto.builder()
                .reply(reply)
                .conversationId(conversation.getId())
                .build();
    }

    @Transactional
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStream(ChatRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation = resolveOrCreateConversation(request, user);
        List<ChatMessage> history = loadRecentHistory(conversation.getId());

        // Save User Message before streaming
        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        String systemPrompt = promptBuilder.buildSystemPrompt(user.getId(), request.getApplicationId(), history);

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = 
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            StringBuilder fullReply = new StringBuilder();
            try {
                // Event 1: status retrieving
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("status").data(Map.of("phase", "retrieving")));

                // Event 2: status thinking
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("status").data(Map.of("phase", "thinking")));

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
                                            .name("status").data(Map.of("phase", "generating")));
                                }
                                fullReply.append(token);
                                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                        .name("token").data(Map.of("text", token)));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        reasoning -> {
                            try {
                                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                        .name("reasoning").data(Map.of("text", reasoning)));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );

                // Save completed assistant message
                if (fullReply.length() > 0) {
                    ChatMessage assistantMsg = ChatMessage.builder()
                            .conversation(conversation)
                            .role(MessageRole.ASSISTANT)
                            .content(fullReply.toString())
                            .build();
                    chatMessageRepository.save(assistantMsg);

                    conversation.setUpdatedAt(LocalDateTime.now());
                    conversationRepository.save(conversation);
                }

                // Event 3: done with conversationId
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("done").data(Map.of("conversationId", conversation.getId())));
                emitter.complete();

                asyncAuditService.logAiInteractionAsync(userEmail, request.getApplicationId(), request.getMessage());
            } catch (Exception ex) {
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error").data(Map.of("status", 500, "message", ex.getMessage() != null ? ex.getMessage() : "Error during streaming")));
                    emitter.completeWithError(ex);
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }

    public static String deriveTitleFromQuestion(String text) {
        if (text == null || text.isBlank()) return "New chat";
        String clean = text.trim();
        String lower = clean.toLowerCase().replaceAll("[^a-z0-9]", "");
        List<String> greetings = List.of("hi", "hello", "hey", "greetings", "yo", "sup", "hola", "goodmorning", "goodafternoon", "goodevening");
        if (greetings.contains(lower)) {
            return "New chat";
        }
        String normalized = clean.replaceAll("\\s+", " ");
        if (normalized.length() <= 40) {
            return normalized;
        }
        return normalized.substring(0, 37) + "...";
    }

    private Conversation resolveOrCreateConversation(ChatRequestDto request, User user) {
        if (request.getConversationId() != null) {
            Conversation conversation = conversationRepository.findByIdAndUserId(request.getConversationId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + request.getConversationId()));
            if ("New chat".equalsIgnoreCase(conversation.getTitle()) || "New conversation".equalsIgnoreCase(conversation.getTitle())) {
                String derived = deriveTitleFromQuestion(request.getMessage());
                if (!"New chat".equalsIgnoreCase(derived)) {
                    conversation.setTitle(derived);
                    conversationRepository.save(conversation);
                }
            }
            return conversation;
        } else {
            String title = deriveTitleFromQuestion(request.getMessage());
            Conversation conversation = Conversation.builder()
                    .user(user)
                    .title(title)
                    .build();
            return conversationRepository.save(conversation);
        }
    }

    private List<ChatMessage> loadRecentHistory(Long conversationId) {
        List<ChatMessage> recent = chatMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);
        List<ChatMessage> chronological = new ArrayList<>(recent);
        Collections.reverse(chronological);
        return chronological;
    }
}
