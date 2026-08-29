package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.dto.UserUpdateDTO;
import com.caioamorimr.ordermanagement.security.JwtUtil;
import com.caioamorimr.ordermanagement.security.SecurityConfig;
import com.caioamorimr.ordermanagement.security.UserDetailsServiceImpl;
import com.caioamorimr.ordermanagement.security.UserSecurity;
import com.caioamorimr.ordermanagement.services.UserService;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.caioamorimr.ordermanagement.util.AuthTestUtils.asAdmin;
import static com.caioamorimr.ordermanagement.util.AuthTestUtils.asRegularUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserResource.class)
@Import(SecurityConfig.class)
class UserResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean(name = "userSecurity")
    private UserSecurity userSecurity;

    private UserDTO userDTO;
    private UserInsertDTO insertDTO;
    private UserUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();

        insertDTO = new UserInsertDTO();
        insertDTO.setName("Caio");
        insertDTO.setEmail("caio@email.com");
        insertDTO.setPhone("988888888");
        insertDTO.setPassword("123456");

        updateDTO = new UserUpdateDTO();
        updateDTO.setName("Caio");
        updateDTO.setEmail("caio@email.com");
        updateDTO.setPhone("988888888");
    }

    @Test
    @DisplayName("GET /users should return 200 with paginated list when caller is admin")
    void findAll_shouldReturn200_whenAdmin() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userDTO)));

        mockMvc.perform(get("/api/v1/users").with(asAdmin(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /users should return 403 when caller is not admin")
    void findAll_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/{id} should return 200 when caller is admin")
    void findById_shouldReturn200_whenUserExists() throws Exception {
        when(userService.findById(1L)).thenReturn(userDTO);

        mockMvc.perform(get("/api/v1/users/1").with(asAdmin(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users/{id} should return 200 when caller is accessing their own profile")
    void findById_shouldReturn200_whenSelfAccess() throws Exception {
        when(userService.findById(1L)).thenReturn(userDTO);
        when(userSecurity.isSelf(eq(1L), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/1").with(asRegularUser(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users/{id} should return 403 when caller tries to access another user's profile")
    void findById_shouldReturn403_whenAccessingAnotherUser() throws Exception {
        when(userSecurity.isSelf(eq(2L), any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/2").with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/{id} should return 404 when user does not exist")
    void findById_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/api/v1/users/99").with(asAdmin(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /users should return 201 when caller is admin and payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(userService.insert(any(UserInsertDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /users should return 403 when caller is not admin")
    void insert_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /users should return 422 when name is blank")
    void insert_shouldReturn422_whenNameIsBlank() throws Exception {
        insertDTO.setName("");

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @DisplayName("POST /users should return 422 when email is invalid")
    void insert_shouldReturn422_whenEmailIsInvalid() throws Exception {
        insertDTO.setEmail("not-an-email");

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("email"));
    }

    @Test
    @DisplayName("PUT /users/{id} should return 200 when caller is admin and payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(userService.update(anyLong(), any(UserUpdateDTO.class))).thenReturn(userDTO);

        mockMvc.perform(put("/api/v1/users/1")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/{id} should return 200 when caller is updating their own profile")
    void update_shouldReturn200_whenSelfAccess() throws Exception {
        when(userService.update(anyLong(), any(UserUpdateDTO.class))).thenReturn(userDTO);
        when(userSecurity.isSelf(eq(1L), any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/users/1")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/{id} should return 403 when caller tries to update another user's profile")
    void update_shouldReturn403_whenUpdatingAnotherUser() throws Exception {
        when(userSecurity.isSelf(eq(2L), any())).thenReturn(false);

        mockMvc.perform(put("/api/v1/users/2")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /users/{id} should return 404 when user does not exist")
    void update_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.update(anyLong(), any(UserUpdateDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/api/v1/users/99")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 204 when caller is admin and user exists")
    void delete_shouldReturn204_whenUserExists() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/v1/users/1").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 403 when caller is not admin")
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1").with(csrf()).with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 404 when user does not exist")
    void delete_shouldReturn404_whenUserNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(userService).delete(99L);

        mockMvc.perform(delete("/api/v1/users/99").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNotFound());
    }
}