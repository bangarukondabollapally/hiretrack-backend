package com.hiretrack.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
import com.hiretrack.user.dto.ProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        profileRepository.deleteAll();
        userRepository.deleteAll();

        // Register User 1
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("prof1@example.com")
                        .password("password123")
                        .build())));

        MvcResult login1Result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("prof1@example.com")
                                .password("password123")
                                .build())))
                .andReturn();

        user1Token = objectMapper.readValue(login1Result.getResponse().getContentAsString(), LoginResponseDto.class).getToken();

        // Register User 2
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("prof2@example.com")
                        .password("password123")
                        .build())));

        MvcResult login2Result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("prof2@example.com")
                                .password("password123")
                                .build())))
                .andReturn();

        user2Token = objectMapper.readValue(login2Result.getResponse().getContentAsString(), LoginResponseDto.class).getToken();
    }

    @Test
    void getAndUpdateProfile_UserIsolationVerified() throws Exception {
        // GET initial profile for User 1 (empty resumeText)
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeText", is("")));

        // PUT update resume for User 1
        ProfileDto updateDto = ProfileDto.builder()
                .resumeText("User 1 Resume Content")
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeText", is("User 1 Resume Content")));

        // Verify User 2's profile is still empty (user isolation)
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeText", is("")));
    }
}
