package com.hiretrack.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
import com.hiretrack.interview.InterviewRepository;
import com.hiretrack.tag.TagRepository;
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
import static org.hamcrest.Matchers.nullValue;
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

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private TagRepository tagRepository;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        interviewRepository.deleteAll();
        tagRepository.deleteAll();
        applicationRepository.deleteAll();
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
        // GET initial profile for User 1 (empty name, resumeText, targetRole, yearsOfExperience, experienceSummary)
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("")))
                .andExpect(jsonPath("$.resumeText", is("")))
                .andExpect(jsonPath("$.targetRole", is("")))
                .andExpect(jsonPath("$.experienceSummary", is("")));

        // PUT update profile for User 1
        ProfileDto updateDto = ProfileDto.builder()
                .name("Jane Doe")
                .resumeText("User 1 Resume Content")
                .targetRole("Frontend Engineer")
                .yearsOfExperience(5)
                .experienceSummary("5 years building React web apps")
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.resumeText", is("User 1 Resume Content")))
                .andExpect(jsonPath("$.targetRole", is("Frontend Engineer")))
                .andExpect(jsonPath("$.yearsOfExperience", is(5)))
                .andExpect(jsonPath("$.experienceSummary", is("5 years building React web apps")));

        // Verify User 2's profile is still empty (user isolation)
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("")))
                .andExpect(jsonPath("$.resumeText", is("")))
                .andExpect(jsonPath("$.targetRole", is("")))
                .andExpect(jsonPath("$.experienceSummary", is("")));
    }

    @Test
    void profile_PartialUpdateAndClearing() throws Exception {
        // Initial full save
        ProfileDto initial = ProfileDto.builder()
                .name("Alice")
                .targetRole("DevOps")
                .yearsOfExperience(8)
                .experienceSummary("Infrastructure engineering")
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk());

        // Partial update: update targetRole only, omit yearsOfExperience and experienceSummary
        String partialJson = "{\"targetRole\":\"Site Reliability Engineer\"}";

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partialJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRole", is("Site Reliability Engineer")))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.yearsOfExperience", is(8)))
                .andExpect(jsonPath("$.experienceSummary", is("Infrastructure engineering")));

        // Clear yearsOfExperience by explicitly sending null, clear experienceSummary by sending ""
        String clearJson = "{\"yearsOfExperience\":null, \"experienceSummary\":\"\"}";

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clearJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRole", is("Site Reliability Engineer")))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.yearsOfExperience", is(nullValue())))
                .andExpect(jsonPath("$.experienceSummary", is("")));
    }

    @Test
    void profile_InvalidValues_Returns400() throws Exception {
        // Invalid yearsOfExperience > 60
        ProfileDto badYears = ProfileDto.builder()
                .yearsOfExperience(75)
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badYears)))
                .andExpect(status().isBadRequest());

        // Invalid negative yearsOfExperience < 0
        ProfileDto negativeYears = ProfileDto.builder()
                .yearsOfExperience(-5)
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeYears)))
                .andExpect(status().isBadRequest());

        // Invalid experienceSummary > 3000 chars
        String longText = "a".repeat(3001);
        ProfileDto badSummary = ProfileDto.builder()
                .experienceSummary(longText)
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badSummary)))
                .andExpect(status().isBadRequest());

        // Invalid avatarPreset key (e.g. preset-99 or invalid string)
        ProfileDto badAvatar = ProfileDto.builder()
                .avatarPreset("preset-99")
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badAvatar)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profile_AvatarPreset_ValidAndUserIsolated() throws Exception {
        // User 1 sets valid avatarPreset "preset-3"
        ProfileDto dto1 = ProfileDto.builder()
                .name("User 1")
                .avatarPreset("preset-3")
                .build();

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreset", is("preset-3")));

        // Verify User 2's avatarPreset is still null (isolated)
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarPreset", is(nullValue())));
    }
}
