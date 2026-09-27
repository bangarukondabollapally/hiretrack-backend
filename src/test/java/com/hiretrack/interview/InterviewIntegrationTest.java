package com.hiretrack.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.application.ApplicationStatus;
import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
import com.hiretrack.interview.dto.InterviewRequestDto;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InterviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    private String user1Token;
    private String user2Token;
    private Application user1App;
    private Interview user1Interview;

    @BeforeEach
    void setUp() throws Exception {
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        // User 1
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("int1@example.com")
                        .password("password123")
                        .build())));

        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("int1@example.com")
                                .password("password123")
                                .build())))
                .andReturn();
        user1Token = objectMapper.readValue(login1.getResponse().getContentAsString(), LoginResponseDto.class).getToken();

        // User 2
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("int2@example.com")
                        .password("password123")
                        .build())));

        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("int2@example.com")
                                .password("password123")
                                .build())))
                .andReturn();
        user2Token = objectMapper.readValue(login2.getResponse().getContentAsString(), LoginResponseDto.class).getToken();

        User user1 = userRepository.findByEmail("int1@example.com").orElseThrow();
        user1App = applicationRepository.save(Application.builder()
                .companyName("TechCorp")
                .jobRole("Developer")
                .status(ApplicationStatus.APPLIED)
                .user(user1)
                .build());

        user1Interview = interviewRepository.save(Interview.builder()
                .round("Technical Screen")
                .interviewDate(LocalDateTime.now().plusDays(3))
                .interviewType("Technical")
                .outcome(InterviewOutcome.PENDING)
                .application(user1App)
                .build());
    }

    @Test
    void userCannotReadOtherUsersInterviews_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/" + user1App.getId() + "/interviews")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCreateInterviewForOtherUsersApplication_ReturnsForbidden() throws Exception {
        InterviewRequestDto dto = InterviewRequestDto.builder()
                .round("System Design")
                .interviewDate(LocalDateTime.now().plusDays(5))
                .interviewType("Technical")
                .build();

        mockMvc.perform(post("/api/applications/" + user1App.getId() + "/interviews")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdateOtherUsersInterview_ReturnsForbidden() throws Exception {
        InterviewRequestDto dto = InterviewRequestDto.builder()
                .round("Updated Round")
                .interviewDate(LocalDateTime.now().plusDays(5))
                .interviewType("HR")
                .build();

        mockMvc.perform(put("/api/applications/" + user1App.getId() + "/interviews/" + user1Interview.getId())
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteOtherUsersInterview_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/applications/" + user1App.getId() + "/interviews/" + user1Interview.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
}
