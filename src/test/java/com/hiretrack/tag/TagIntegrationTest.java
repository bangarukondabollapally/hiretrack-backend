package com.hiretrack.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.application.ApplicationStatus;
import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private TagRepository tagRepository;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;
    private Application user1App;
    private Tag user1Tag;
    private Tag user2Tag;

    @BeforeEach
    void setUp() throws Exception {
        applicationRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();

        // Register & Login User 1
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("taguser1@example.com")
                        .password("password123")
                        .build())));

        MvcResult login1Result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("taguser1@example.com")
                                .password("password123")
                                .build())))
                .andReturn();
        user1Token = objectMapper.readValue(login1Result.getResponse().getContentAsString(), LoginResponseDto.class).getToken();
        user1 = userRepository.findByEmail("taguser1@example.com").orElseThrow();

        // Register & Login User 2
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequestDto.builder()
                        .email("taguser2@example.com")
                        .password("password123")
                        .build())));

        MvcResult login2Result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                                .email("taguser2@example.com")
                                .password("password123")
                                .build())))
                .andReturn();
        user2Token = objectMapper.readValue(login2Result.getResponse().getContentAsString(), LoginResponseDto.class).getToken();
        user2 = userRepository.findByEmail("taguser2@example.com").orElseThrow();

        // User 1 Application
        user1App = applicationRepository.save(Application.builder()
                .companyName("TechCorp")
                .jobRole("Backend Engineer")
                .status(ApplicationStatus.APPLIED)
                .appliedDate(LocalDate.now())
                .user(user1)
                .build());

        // Create Tag for User 1
        user1Tag = tagRepository.save(Tag.builder()
                .name("Remote")
                .user(user1)
                .build());

        // Create Tag for User 2
        user2Tag = tagRepository.save(Tag.builder()
                .name("Urgent")
                .user(user2)
                .build());
    }

    @Test
    void getUserTags_OnlyReturnsOwnTags() throws Exception {
        mockMvc.perform(get("/api/tags")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Remote")));
    }

    @Test
    void userCannotAssignOtherUsersTag_ReturnsForbidden() throws Exception {
        // User 1 tries to assign User 2's tag to User 1's application
        mockMvc.perform(post("/api/applications/" + user1App.getId() + "/tags/" + user2Tag.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAssignTagToOtherUsersApplication_ReturnsForbidden() throws Exception {
        // User 2 tries to assign User 2's tag to User 1's application
        mockMvc.perform(post("/api/applications/" + user1App.getId() + "/tags/" + user2Tag.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotRemoveTagFromOtherUsersApplication_ReturnsForbidden() throws Exception {
        // Assign tag to User 1's app as User 1
        mockMvc.perform(post("/api/applications/" + user1App.getId() + "/tags/" + user1Tag.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // User 2 tries to remove User 1's tag from User 1's application
        mockMvc.perform(delete("/api/applications/" + user1App.getId() + "/tags/" + user1Tag.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
}
