package com.hiretrack.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.application.dto.ApplicationRequestDto;
import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
import com.hiretrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        // Register User 1
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("user1@example.com")
                        .password("password123")
                        .build())));

        MvcResult login1Result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("user1@example.com")
                                .password("password123")
                                .build())))
                .andReturn();

        LoginResponseDto login1 = objectMapper.readValue(login1Result.getResponse().getContentAsString(), LoginResponseDto.class);
        user1Token = login1.getToken();

        // Register User 2
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("user2@example.com")
                        .password("password123")
                        .build())));

        MvcResult login2Result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("user2@example.com")
                                .password("password123")
                                .build())))
                .andReturn();

        LoginResponseDto login2 = objectMapper.readValue(login2Result.getResponse().getContentAsString(), LoginResponseDto.class);
        user2Token = login2.getToken();
    }

    @Test
    void createAndGetApplications_UserIsolationVerified() throws Exception {
        // User 1 creates application
        ApplicationRequestDto app1 = ApplicationRequestDto.builder()
                .companyName("Company 1")
                .jobRole("Software Engineer")
                .status(ApplicationStatus.APPLIED)
                .appliedDate(LocalDate.now())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(app1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName", is("Company 1")))
                .andReturn();

        // Extract ID
        String json = createResult.getResponse().getContentAsString();
        Long app1Id = objectMapper.readTree(json).get("id").asLong();

        // User 1 can see application
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName", is("Company 1")));

        // User 2 sees 0 applications
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // User 2 tries to access User 1's application directly -> 403 Forbidden
        mockMvc.perform(get("/api/applications/" + app1Id)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
}
