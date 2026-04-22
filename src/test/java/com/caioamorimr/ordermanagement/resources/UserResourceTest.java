package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.services.UserService;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserResource.class)
class UserResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDTO userDTO;
    private UserInsertDTO insertDTO;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();

        insertDTO = new UserInsertDTO();
        insertDTO.setName("Caio");
        insertDTO.setEmail("caio@email.com");
        insertDTO.setPhone("988888888");
        insertDTO.setPassword("123456");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /users should return 200 with paginated list")
    void findAll_shouldReturn200() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userDTO)));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /users/{id} should return 200 when user exists")
    void findById_shouldReturn200_whenUserExists() throws Exception {
        when(userService.findById(1L)).thenReturn(userDTO);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /users/{id} should return 404 when user does not exist")
    void findById_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /users should return 201 when payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(userService.insert(any(UserInsertDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /users should return 422 when name is blank")
    void insert_shouldReturn422_whenNameIsBlank() throws Exception {
        insertDTO.setName("");

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /users should return 422 when email is invalid")
    void insert_shouldReturn422_whenEmailIsInvalid() throws Exception {
        insertDTO.setEmail("not-an-email");

        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("email"));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /users/{id} should return 204 when user exists")
    void delete_shouldReturn204_whenUserExists() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/users/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /users/{id} should return 404 when user does not exist")
    void delete_shouldReturn404_whenUserNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(userService).delete(99L);

        mockMvc.perform(delete("/users/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
