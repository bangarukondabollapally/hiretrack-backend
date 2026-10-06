package com.hiretrack.opening;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.auth.JwtService;
import com.hiretrack.opening.dto.PlacementOpeningRequestDto;
import com.hiretrack.user.Role;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PlacementOpeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlacementOpeningRepository openingRepository;

    @Autowired
    private UserTrackedOpeningRepository trackedOpeningRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hiretrack.application.ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;
    private User adminUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        trackedOpeningRepository.deleteAll();
        openingRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .email("admin@hiretrack.local")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash(passwordEncoder.encode("student123"))
                .role(Role.USER)
                .build());

        adminToken = jwtService.generateToken(adminUser.getEmail(), adminUser.getId());
        userToken = jwtService.generateToken(studentUser.getEmail(), studentUser.getId());
    }

    @Test
    void adminCanCreateUpdateCloseAndDeletePlacementOpening() throws Exception {
        PlacementOpeningRequestDto createDto = PlacementOpeningRequestDto.builder()
                .companyName("Acme Corp")
                .jobRole("Software Engineer")
                .jobType("Full-time")
                .location("Bengaluru")
                .workMode("Hybrid")
                .packageDetails("12 LPA")
                .eligibility("B.Tech CSE 2026")
                .yearOfStudy("4th Year")
                .seats(10)
                .deadline(LocalDate.now().plusDays(30))
                .description("Build scalable services.")
                .applicationLink("careers.acme.com/jobs/1")
                .status(OpeningStatus.OPEN)
                .build();

        String createResponse = mockMvc.perform(post("/api/admin/openings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.companyName", is("Acme Corp")))
                .andExpect(jsonPath("$.applicationLink", is("https://careers.acme.com/jobs/1")))
                .andExpect(jsonPath("$.seats", is(10)))
                .andExpect(jsonPath("$.yearOfStudy", is("4th Year")))
                .andReturn().getResponse().getContentAsString();

        Long openingId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update
        createDto.setCompanyName("Acme Technologies");
        createDto.setApplicationLink("https://careers.acme.com/jobs/1");
        mockMvc.perform(put("/api/admin/openings/" + openingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName", is("Acme Technologies")));

        // Close
        mockMvc.perform(put("/api/admin/openings/" + openingId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));

        // Delete
        mockMvc.perform(delete("/api/admin/openings/" + openingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void studentCanTrackAndUntrackOpening() throws Exception {
        PlacementOpening opening = openingRepository.save(PlacementOpening.builder()
                .companyName("Microsoft")
                .jobRole("Software Engineer")
                .applicationLink("https://careers.microsoft.com")
                .status(OpeningStatus.OPEN)
                .build());

        // Track opening
        mockMvc.perform(post("/api/openings/" + opening.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTracked", is(true)));

        // Verify GET /api/openings returns isTracked = true
        mockMvc.perform(get("/api/openings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isTracked", is(true)));

        // Untrack opening
        mockMvc.perform(delete("/api/openings/" + opening.getId() + "/track")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTracked", is(false)));

        // Verify GET /api/openings returns isTracked = false
        mockMvc.perform(get("/api/openings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isTracked", is(false)));
    }

    @Test
    void studentCallingAdminEndpoints_Returns403Forbidden() throws Exception {
        PlacementOpeningRequestDto createDto = PlacementOpeningRequestDto.builder()
                .companyName("Hack Corp")
                .jobRole("Hacker")
                .applicationLink("https://hack.com")
                .build();

        mockMvc.perform(post("/api/admin/openings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUser_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/openings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOpening_InvalidApplicationLink_Returns400() throws Exception {
        PlacementOpeningRequestDto invalidDto = PlacementOpeningRequestDto.builder()
                .companyName("Invalid Link Corp")
                .jobRole("Developer")
                .applicationLink("javascript:alert(1)")
                .build();

        mockMvc.perform(post("/api/admin/openings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}
