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
        when(groqClient.generateResponse(anyString(), anyString(), any()))
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
    void chat_EmptyMessageAndNoAttachments_Returns400() throws Exception {
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
        when(groqClient.generateResponse(anyString(), anyString(), any()))
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
            java.util.function.Consumer<String> onToken = invocation.getArgument(3);
            java.util.function.Consumer<String> onReasoning = invocation.getArgument(4);
            onReasoning.accept("Analyzing application details");
            onToken.accept("Hello");
            onToken.accept(" world!");
            return null;
        }).when(groqClient).streamResponse(anyString(), anyString(), any(), any(), any());

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
                .when(groqClient).streamResponse(anyString(), anyString(), any(), any(), any());

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
            java.util.function.Consumer<String> onToken = invocation.getArgument(3);
            onToken.accept("Thank\u2011you ");
            onToken.accept("for your time! ");
            onToken.accept("\uD83D\uDE80 ");
            onToken.accept("(to\u2011on\u2011site)");
            return null;
        }).when(groqClient).streamResponse(anyString(), anyString(), any(), any(), any());

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

    @Test
    void chat_ValidTextAttachment_Success() throws Exception {
        when(groqClient.generateResponse(anyString(), anyString(), any()))
                .thenReturn("Processed resume attachment");

        com.hiretrack.ai.dto.ChatAttachmentDto att = com.hiretrack.ai.dto.ChatAttachmentDto.builder()
                .type("text")
                .name("resume.pdf")
                .content("Extracted text from resume")
                .build();

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Review this resume")
                .attachments(java.util.List.of(att))
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Processed resume attachment")));
    }

    @Test
    void chat_ValidImageAttachment_Success() throws Exception {
        when(groqClient.generateResponse(anyString(), anyString(), any()))
                .thenReturn("Analyzed screenshot");

        // 1x1 PNG base64
        String validPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        com.hiretrack.ai.dto.ChatAttachmentDto att = com.hiretrack.ai.dto.ChatAttachmentDto.builder()
                .type("image")
                .name("shot.png")
                .mimeType("image/png")
                .data(validPngBase64)
                .build();

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Describe this shot")
                .attachments(java.util.List.of(att))
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Analyzed screenshot")));
    }

    @Test
    void chat_WrongMagicBytes_Returns400() throws Exception {
        // Text encoded as base64 claiming to be JPEG
        String invalidJpegBase64 = java.util.Base64.getEncoder().encodeToString("not-an-image-file".getBytes());

        com.hiretrack.ai.dto.ChatAttachmentDto att = com.hiretrack.ai.dto.ChatAttachmentDto.builder()
                .type("image")
                .name("shot.jpg")
                .mimeType("image/jpeg")
                .data(invalidJpegBase64)
                .build();

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Check image")
                .attachments(java.util.List.of(att))
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Image content does not match specified MIME type")));
    }

    @Test
    void chat_SvgRejected_Returns400() throws Exception {
        String svgBase64 = java.util.Base64.getEncoder().encodeToString("<svg xmlns=\"http://www.w3.org/2000/svg\"><circle r=\"10\"/></svg>".getBytes());

        com.hiretrack.ai.dto.ChatAttachmentDto att = com.hiretrack.ai.dto.ChatAttachmentDto.builder()
                .type("image")
                .name("diagram.svg")
                .mimeType("image/svg+xml")
                .data(svgBase64)
                .build();

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Check svg")
                .attachments(java.util.List.of(att))
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("SVG and vector graphics are not supported")));
    }

    @Test
    void chat_TooManyAttachments_Returns400() throws Exception {
        java.util.List<com.hiretrack.ai.dto.ChatAttachmentDto> list = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            list.add(com.hiretrack.ai.dto.ChatAttachmentDto.builder()
                    .type("text")
                    .name("doc" + i + ".txt")
                    .content("content " + i)
                    .build());
        }

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Too many files")
                .attachments(list)
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Maximum 5 attachments allowed")));
    }

    @Test
    void chat_ImageWithoutVisionModelConfigured_Returns400() throws Exception {
        when(groqClient.generateResponse(anyString(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("Image analysis is not enabled"));

        String validPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        com.hiretrack.ai.dto.ChatAttachmentDto att = com.hiretrack.ai.dto.ChatAttachmentDto.builder()
                .type("image")
                .name("shot.png")
                .mimeType("image/png")
                .data(validPngBase64)
                .build();

        ChatRequestDto request = ChatRequestDto.builder()
                .message("Vision test")
                .attachments(java.util.List.of(att))
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Image analysis is not enabled")));
    }
}


