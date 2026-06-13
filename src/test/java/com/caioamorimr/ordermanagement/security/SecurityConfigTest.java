package com.caioamorimr.ordermanagement.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
