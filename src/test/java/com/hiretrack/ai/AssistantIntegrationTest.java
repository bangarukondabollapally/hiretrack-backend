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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
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

    @Test
    void chatStream_Unauthenticated_Returns401Or403() throws Exception {
        ChatRequestDto request = ChatRequestDto.builder()
                .message("Hello assistant")
                .build();

        mockMvc.perform(post("/api/assistant/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void chatStream_OwnershipFailure_Returns403Or404() throws Exception {
        ChatRequestDto request = ChatRequestDto.builder()
                .message("Prepare for non owned app")
                .applicationId(999999L)
                .build();

        mockMvc.perform(post("/api/assistant/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void chatStream_Success() throws Exception {
        doAnswer(invocation -> {
            java.util.function.Consumer<String> onToken = invocation.getArgument(2);
            java.util.function.Consumer<String> onReasoning = invocation.getArgument(3);
            onReasoning.accept("Analyzing application details");
            onToken.accept("Hello");
            onToken.accept(" world!");
            return null;
        }).when(groqClient).streamResponse(anyString(), anyString(), any(), any());

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Hi")
                .build();

        MvcResult result = mockMvc.perform(post("/api/assistant/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/event-stream")))
                .andReturn();

        result.getAsyncResult(2000L);

        String content = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("event:status"));
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("event:token"));
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("event:reasoning"));
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("event:done"));
    }

    @Test
    void chatStream_GroqFailureMidStream_EmitsErrorEvent() throws Exception {
        doThrow(new AiServiceException("Mid-stream connection broken"))
                .when(groqClient).streamResponse(anyString(), anyString(), any(), any());

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Hi")
                .build();

        MvcResult result = mockMvc.perform(post("/api/assistant/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        try {
            mockMvc.perform(asyncDispatch(result));
        } catch (Exception ignored) {
        }

        String content = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("event:error"));
    }

    @Test
    void chatStream_MultiByteCharacters_PreservesExactText() throws Exception {
        final String multiByteText = "Thank\u2011you for your time! \uD83D\uDE80 (to\u2011on\u2011site)";
        doAnswer(invocation -> {
            java.util.function.Consumer<String> onToken = invocation.getArgument(2);
            onToken.accept("Thank\u2011you ");
            onToken.accept("for your time! ");
            onToken.accept("\uD83D\uDE80 ");
            onToken.accept("(to\u2011on\u2011site)");
            return null;
        }).when(groqClient).streamResponse(anyString(), anyString(), any(), any());

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Multi-byte test")
                .build();

        MvcResult result = mockMvc.perform(post("/api/assistant/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        result.getAsyncResult(2000L);

        String content = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        StringBuilder sb = new StringBuilder();

        for (String line : content.split("\n")) {
            if (line.startsWith("data:") && line.contains("\"text\":")) {
                String jsonStr = line.substring(5).trim();
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(jsonStr);
                if (node.has("text")) {
                    sb.append(node.get("text").asText());
                }
            }
        }

        org.junit.jupiter.api.Assertions.assertEquals(multiByteText, sb.toString());
    }
}


