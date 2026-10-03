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
                .deadline(LocalDate.now().plusDays(30))
                .description("Build scalable services.")
                .applicationLink("https://careers.acme.com/jobs/1")
                .status(OpeningStatus.OPEN)
                .build();

        String createResponse = mockMvc.perform(post("/api/admin/openings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.companyName", is("Acme Corp")))
                .andReturn().getResponse().getContentAsString();

        Long openingId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update
        createDto.setCompanyName("Acme Technologies");
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
    void studentCanViewOpenPlacementOpenings() throws Exception {
        PlacementOpening openOpening = openingRepository.save(PlacementOpening.builder()
                .companyName("Google")
                .jobRole("Frontend Engineer")
                .applicationLink("https://careers.google.com")
                .status(OpeningStatus.OPEN)
                .build());

        PlacementOpening closedOpening = openingRepository.save(PlacementOpening.builder()
                .companyName("Legacy Systems")
                .jobRole("DBA")
                .applicationLink("https://legacy.com")
                .status(OpeningStatus.CLOSED)
                .build());

        // Default student view — returns only open openings
        mockMvc.perform(get("/api/openings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName", is("Google")));

        // Student view with includeClosed=true
        mockMvc.perform(get("/api/openings?includeClosed=true")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createOpening_InvalidApplicationLink_Returns400() throws Exception {
        PlacementOpeningRequestDto invalidDto = PlacementOpeningRequestDto.builder()
                .companyName("Invalid Link Corp")
                .jobRole("Developer")
                .applicationLink("ftp://invalid-link.com")
                .build();

        mockMvc.perform(post("/api/admin/openings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}
