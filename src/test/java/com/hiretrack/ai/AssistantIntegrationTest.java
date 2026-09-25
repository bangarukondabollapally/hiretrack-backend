package com.hiretrack.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.ai.dto.ChatRequestDto;
import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
import com.hiretrack.common.exception.AiServiceException;
import com.hiretrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AssistantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hiretrack.application.ApplicationRepository applicationRepository;

    @MockBean
    private GroqClient groqClient;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequestDto reg = RegisterRequestDto.builder()
                .email("ai.user@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("ai.user@example.com")
                                .password("password123")
                                .build())))
                .andReturn();

        LoginResponseDto login = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponseDto.class);
        token = login.getToken();
    }

    @Test
    void chat_Success() throws Exception {
        when(groqClient.generateResponse(anyString(), anyString()))
                .thenReturn("Here is tailored advice for your interview.");

        ChatRequestDto request = ChatRequestDto.builder()
                .message("How can I prepare for Google?")
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Here is tailored advice for your interview.")));
    }

    @Test
    void chat_EmptyMessage_Returns400() throws Exception {
        ChatRequestDto request = ChatRequestDto.builder()
                .message("")
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_GroqFailure_Returns502() throws Exception {
        when(groqClient.generateResponse(anyString(), anyString()))
                .thenThrow(new AiServiceException("Groq API error"));

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Hello assistant")
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status", is(502)))
                .andExpect(jsonPath("$.message", is("Groq API error")));
    }
}
