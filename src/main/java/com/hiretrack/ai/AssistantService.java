package com.hiretrack.ai;

import com.hiretrack.ai.dto.ChatRequestDto;
import com.hiretrack.ai.dto.ChatResponseDto;
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

    @Transactional(readOnly = true)
    public ChatResponseDto chat(ChatRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String systemPrompt = promptBuilder.buildSystemPrompt(user.getId(), request.getApplicationId());
        String reply = groqClient.generateResponse(systemPrompt, request.getMessage());

        return ChatResponseDto.builder()
                .reply(reply)
                .build();
    }
}
