package com.hiretrack.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.ai.GroqClient;
import com.hiretrack.ai.dto.ChatRequestDto;
import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.application.ApplicationStatus;
import com.hiretrack.assistant.dto.ConversationRequestDto;
import com.hiretrack.opening.OpeningStatus;
import com.hiretrack.opening.PlacementOpening;
import com.hiretrack.opening.PlacementOpeningRepository;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AssistantHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PlacementOpeningRepository openingRepository;

    @MockBean
    private GroqClient groqClient;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        applicationRepository.deleteAll();
        openingRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("student@test.com")
                .passwordHash("hashedpass")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other@test.com")
                .passwordHash("hashedpass")
                .build());

        Mockito.when(groqClient.generateResponse(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn("This is a mock assistant response.");
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void testConversationCrudAndOwnershipIsolation() throws Exception {
        // 1. Create conversation
        ConversationRequestDto createDto = ConversationRequestDto.builder().title("Amazon Prep").build();
        String json = mockMvc.perform(post("/api/assistant/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Amazon Prep")))
                .andReturn().getResponse().getContentAsString();

        Long convId = objectMapper.readTree(json).get("id").asLong();

        // 2. List conversations
        mockMvc.perform(get("/api/assistant/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Amazon Prep")));

        // 3. Rename conversation
        ConversationRequestDto renameDto = ConversationRequestDto.builder().title("Amazon Technical Prep").build();
        mockMvc.perform(put("/api/assistant/conversations/" + convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Amazon Technical Prep")));

        // 4. Access conversation as another user (expect 404 / ownership isolation)
        mockMvc.perform(get("/api/assistant/conversations/" + convId + "/messages")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("other@test.com")))
                .andExpect(status().isNotFound());

        // 5. Delete conversation
        mockMvc.perform(delete("/api/assistant/conversations/" + convId))
                .andExpect(status().isNoContent());

        assertEquals(0, conversationRepository.count());
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void testChatPersistenceAndAutoCreation() throws Exception {
        ChatRequestDto chatReq = ChatRequestDto.builder()
                .message("How do I structure my resume?")
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("This is a mock assistant response.")))
                .andExpect(jsonPath("$.conversationId", notNullValue()));

        List<Conversation> convs = conversationRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId());
        assertEquals(1, convs.size());

        List<ChatMessage> msgs = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(convs.get(0).getId());
        assertEquals(2, msgs.size());
        assertEquals(MessageRole.USER, msgs.get(0).getRole());
        assertEquals("How do I structure my resume?", msgs.get(0).getContent());
        assertEquals(MessageRole.ASSISTANT, msgs.get(1).getRole());
        assertEquals("This is a mock assistant response.", msgs.get(1).getContent());
    }

    @Test
    @WithMockUser(username = "student@test.com")
    void testPlacementOpeningContextInPrompt() throws Exception {
        PlacementOpening opening = openingRepository.save(PlacementOpening.builder()
                .companyName("Acme Tech")
                .jobRole("SDE 1")
                .status(OpeningStatus.OPEN)
                .applicationLink("https://acme.com/apply")
                .packageDetails("15 LPA")
                .eligibility("B.Tech 2026")
                .deadline(LocalDate.now().plusDays(10))
                .description("Build high throughput microservices.")
                .build());

        Application app = applicationRepository.save(Application.builder()
                .user(testUser)
                .companyName("Acme Tech")
                .jobRole("SDE 1")
                .status(ApplicationStatus.APPLIED)
                .placementOpeningId(opening.getId())
                .build());

        ChatRequestDto chatReq = ChatRequestDto.builder()
                .message("Help me evaluate my fit for Acme Tech")
                .applicationId(app.getId())
                .build();

        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk());

        // Verify prompt builder passed placement details to groq client
        Mockito.verify(groqClient).generateResponse(
                Mockito.contains("<PLACEMENT_OPENING_DETAILS>"),
                Mockito.eq("Help me evaluate my fit for Acme Tech"),
                Mockito.any()
        );
    }
}
