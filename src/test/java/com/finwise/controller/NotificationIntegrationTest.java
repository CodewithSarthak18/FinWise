package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.entity.Notification;
import com.finwise.entity.NotificationType;
import com.finwise.entity.User;
import com.finwise.repository.NotificationRepository;
import com.finwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        notificationRepository.deleteAll();

        String uniqueSuffix = String.valueOf(System.nanoTime());
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Aarav")
                .lastName("Kapoor")
                .email("aarav." + uniqueSuffix + "@finwise.test")
                .password("SecureFinPass#2026")
                .currencyCode("INR")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        token = "Bearer " + objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        testUser = userRepository.findByEmail("aarav." + uniqueSuffix + "@finwise.test").orElseThrow();
    }

    @Test
    @DisplayName("Unauthenticated request to notifications should return 401 Unauthorized")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Fetch notifications and unread count for user")
    void fetchNotificationsAndUnreadCount() throws Exception {
        Notification n1 = notificationRepository.save(Notification.builder()
                .user(testUser)
                .title("Salary Credited")
                .message("Your monthly salary of ₹1,25,000 has been credited.")
                .type(NotificationType.SYSTEM_ALERT)
                .read(false)
                .createdAt(Instant.now())
                .build());

        Notification n2 = notificationRepository.save(Notification.builder()
                .user(testUser)
                .title("Budget Warning")
                .message("You have utilized 85% of Dining budget.")
                .type(NotificationType.BUDGET_WARNING)
                .read(true)
                .createdAt(Instant.now().minusSeconds(3600))
                .build());

        // Test Unread Count
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.count", is(1)));

        // Test List Notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", token)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title", notNullValue()));
    }

    @Test
    @DisplayName("Mark single notification as read and mark all as read")
    void markAsReadAndMarkAll() throws Exception {
        Notification n1 = notificationRepository.save(Notification.builder()
                .user(testUser)
                .title("Goal Milestone")
                .message("Emergency fund reached 50%.")
                .type(NotificationType.GOAL_REACHED)
                .read(false)
                .createdAt(Instant.now())
                .build());

        Notification n2 = notificationRepository.save(Notification.builder()
                .user(testUser)
                .title("Bill Due")
                .message("Electricity bill due tomorrow.")
                .type(NotificationType.RECURRING_DUE)
                .read(false)
                .createdAt(Instant.now().minusSeconds(100))
                .build());

        // Mark n1 as read
        mockMvc.perform(patch("/api/v1/notifications/" + n1.getId() + "/read")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertThat(notificationRepository.findById(n1.getId()).get().isRead()).isTrue();
        assertThat(notificationRepository.findById(n2.getId()).get().isRead()).isFalse();

        // Mark all as read
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertThat(notificationRepository.findById(n2.getId()).get().isRead()).isTrue();
    }
}
