package com.hiretrack.assistant;

import com.hiretrack.assistant.dto.ChatMessageResponseDto;
import com.hiretrack.assistant.dto.ConversationRequestDto;
import com.hiretrack.assistant.dto.ConversationResponseDto;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssistantHistoryService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ConversationResponseDto> getConversations(String userEmail) {
        User user = getUserByEmail(userEmail);
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::mapToConversationDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponseDto createConversation(ConversationRequestDto dto, String userEmail) {
        User user = getUserByEmail(userEmail);

        String title = (dto != null && dto.getTitle() != null && !dto.getTitle().trim().isEmpty())
                ? dto.getTitle().trim()
                : "New chat";

        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }

        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title)
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return mapToConversationDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDto> getMessages(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(this::mapToMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponseDto renameConversation(Long id, ConversationRequestDto dto, String userEmail) {
        User user = getUserByEmail(userEmail);
        Conversation conversation = conversationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));

        String title = (dto != null && dto.getTitle() != null && !dto.getTitle().trim().isEmpty())
                ? dto.getTitle().trim()
                : "New chat";

        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }

        conversation.setTitle(title);
        Conversation updated = conversationRepository.save(conversation);
        return mapToConversationDto(updated);
    }

    @Transactional
    public void deleteConversation(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Conversation conversation = conversationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));

        conversationRepository.delete(conversation);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public ConversationResponseDto mapToConversationDto(Conversation conversation) {
        return ConversationResponseDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    public ChatMessageResponseDto mapToMessageDto(ChatMessage message) {
        return ChatMessageResponseDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
