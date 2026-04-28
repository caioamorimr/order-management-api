package com.caioamorimr.ordermanagement.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoints should be accessible without authentication")
    void publicEndpoints_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // Test categories endpoint
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk());

        // Test products endpoint
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());

        // Test orders endpoint
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());

        // Test users endpoint
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }
}
