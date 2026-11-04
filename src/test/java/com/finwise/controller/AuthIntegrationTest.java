package com.finwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finwise.dto.account.AccountRequest;
import com.finwise.dto.auth.LoginRequest;
import com.finwise.dto.auth.RefreshTokenRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.entity.AccountType;
import com.finwise.entity.RefreshToken;
import com.finwise.entity.User;
import com.finwise.repository.RefreshTokenRepository;
import com.finwise.repository.UserRepository;
import com.finwise.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("Complete Auth Flow: Register -> Login -> Profile Inspection -> Error Cases")
    void verifyCompleteAuthFlow() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Vikram")
                .lastName("Malhotra")
                .email("vikram.malhotra@finwise.in")
                .password("SecureFinPass#2026")
                .currencyCode("INR")
                .build();

        // 1. Register new user
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.email", is("vikram.malhotra@finwise.in")))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        // 2. Duplicate registration returns 409 Conflict
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("CONFLICT")));

        // 3. Login with correct credentials
        LoginRequest loginReq = LoginRequest.builder()
                .email("vikram.malhotra@finwise.in")
                .password("SecureFinPass#2026")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).path("data").path("accessToken").asText();

        // 4. Access protected profile endpoint with Bearer token
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("vikram.malhotra@finwise.in")))
                .andExpect(jsonPath("$.data.firstName", is("Vikram")))
                .andExpect(jsonPath("$.data.currencyCode", is("INR")));

        // 5. Access without token returns 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security: BCrypt Password Hashing and No Plain-Text / Hash Exposure")
    void verifyPasswordHashingAndNoExposure() throws Exception {
        String rawPassword = "StrongPassword@987";
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Ananya")
                .lastName("Iyer")
                .email("ananya.iyer@finwise.in")
                .password(rawPassword)
                .currencyCode("INR")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        // Verify Database Persistence: Never plain text, starts with $2a$ (BCrypt)
        User persistedUser = userRepository.findByEmail("ananya.iyer@finwise.in").orElseThrow();
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(persistedUser.getPasswordHash()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches(rawPassword, persistedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", persistedUser.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Security: Refresh Token Rotation and Revoked Token Reuse Detection")
    void verifyRefreshTokenRotationAndReuseProtection() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Rohan")
                .lastName("Verma")
                .email("rohan.verma@finwise.in")
                .password("RohanPass#2026")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String regJson = registerResult.getResponse().getContentAsString();
        String initialRefreshToken = objectMapper.readTree(regJson).path("data").path("refreshToken").asText();

        // 1. Refresh token successfully rotates: returns new access token & new refresh token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", not(equalTo(initialRefreshToken))))
                .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // 2. Re-using the OLD (already rotated) refresh token triggers suspicious reuse detection
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Compromised refresh token used")));

        // 3. Confirm all user sessions are revoked
        User user = userRepository.findByEmail("rohan.verma@finwise.in").orElseThrow();
        List<RefreshToken> allTokens = refreshTokenRepository.findByUser(user);
        assertThat(allTokens).isNotEmpty();
        assertThat(allTokens).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("Security: Secure Logout Revokes Refresh Tokens")
    void verifySecureLogout() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .firstName("Pooja")
                .lastName("Nair")
                .email("pooja.nair@finwise.in")
                .password("PoojaPass#2026")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String regJson = registerResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(regJson).path("data").path("accessToken").asText();

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Logged out successfully")));

        // Verify tokens revoked in database
        User user = userRepository.findByEmail("pooja.nair@finwise.in").orElseThrow();
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("Security: User Data Isolation - User B Cannot Access or Mutate User A's Accounts")
    void verifyUserDataIsolationBetweenTenants() throws Exception {
        // Register User A
        RegisterRequest userAReq = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Agarwal")
                .email("alice.agarwal@finwise.in")
                .password("AlicePass#2026")
                .build();

        MvcResult userAResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userAReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String tokenA = objectMapper.readTree(userAResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // User A creates a bank account
        AccountRequest accRequest = AccountRequest.builder()
                .accountName("Alice Private Savings")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("250000.00"))
                .institutionName("HDFC Bank")
                .build();

        MvcResult accResult = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long aliceAccountId = objectMapper.readTree(accResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Register User B
        RegisterRequest userBReq = RegisterRequest.builder()
                .firstName("Bob")
                .lastName("Banerjee")
                .email("bob.banerjee@finwise.in")
                .password("BobPass#2026")
                .build();

        MvcResult userBResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String tokenB = objectMapper.readTree(userBResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // User B attempts to view Alice's account -> 404 Not Found (tenant-isolated)
        mockMvc.perform(get("/api/v1/accounts/" + aliceAccountId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("NOT_FOUND")));

        // User B attempts to delete Alice's account -> 404 Not Found
        mockMvc.perform(delete("/api/v1/accounts/" + aliceAccountId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("Security: Protected REST APIs Reject Unauthorized and Malformed Token Requests")
    void verifyProtectedEndpointsRejectUnauthorized() throws Exception {
        // Without Authorization Header
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/budgets/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));

        // With Malformed or Invalid Token
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Validation Error: Register with invalid email, short password, and blank fields")
    void verifyValidationFailsForInvalidData() throws Exception {
        RegisterRequest invalidReq = RegisterRequest.builder()
                .firstName("")
                .lastName("")
                .email("not-an-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", is("Password must be between 8 and 100 characters")))
                .andExpect(jsonPath("$.validationErrors.firstName", is("First name is required")))
                .andExpect(jsonPath("$.validationErrors.lastName", is("Last name is required")));
    }
}
