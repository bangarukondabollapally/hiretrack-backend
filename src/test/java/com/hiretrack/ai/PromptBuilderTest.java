package com.hiretrack.ai;

import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.user.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PromptBuilderTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private PromptBuilder promptBuilder;

    @Test
    void systemPrompt_ContainsTableRule() {
        when(profileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        String prompt = promptBuilder.buildSystemPrompt(1L, null);

        assertTrue(prompt.contains("If you use a table, the separator row must have exactly as many cells as the header row, every row must be on its own line, and never use HTML tags such as <br>; put multiple points in one cell separated by '; '."),
                "System prompt should contain the exact table rule instruction.");
        assertTrue(prompt.contains("Prefer lists, use a table only when it genuinely helps or the user asks."),
                "System prompt should retain preference for lists.");
    }
}
