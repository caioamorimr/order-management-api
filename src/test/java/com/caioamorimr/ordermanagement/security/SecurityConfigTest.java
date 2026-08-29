package com.caioamorimr.ordermanagement.security;

import com.caioamorimr.ordermanagement.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Protected endpoints should return 401 for unauthenticated requests")
    void protectedEndpoints_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth endpoints should stay public even without authentication")
    @Transactional
    void authEndpoints_shouldNotReturn401_whenNotAuthenticated() throws Exception {
        String loginBody = """
                {"email": "caio@email.com", "password": "123456"}
                """;
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readValue(loginResponse, TokenResponse.class).refreshToken();

        String registerBody = """
                {"name": "New User", "email": "new-user-%d@email.com", "phone": "999999999", "password": "123456"}
                """.formatted(System.currentTimeMillis());
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String refreshRequestBody = """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String rotatedRefreshToken = objectMapper.readValue(refreshResponse, TokenResponse.class).refreshToken();

        String logoutRequestBody = """
                {"refreshToken": "%s"}
                """.formatted(rotatedRefreshToken);
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutRequestBody))
                .andExpect(status().isNoContent());
    }
}