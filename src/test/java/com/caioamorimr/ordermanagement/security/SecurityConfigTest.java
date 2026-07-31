package com.caioamorimr.ordermanagement.security;

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

    @Test
    @DisplayName("Protected endpoints should return 401 for unauthenticated requests")
    void protectedEndpoints_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/products")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth endpoints should stay public even without authentication")
    @Transactional
    void authEndpoints_shouldNotReturn401_whenNotAuthenticated() throws Exception {
        String loginBody = """
                {"email": "caio@email.com", "password": "123456"}
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());

        String registerBody = """
                {"name": "New User", "email": "new-user-%d@email.com", "phone": "999999999", "password": "123456"}
                """.formatted(System.currentTimeMillis());
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());
    }
}