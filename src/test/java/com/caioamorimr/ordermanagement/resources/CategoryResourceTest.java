package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.CategoryDTO;
import com.caioamorimr.ordermanagement.security.JwtUtil;
import com.caioamorimr.ordermanagement.security.SecurityConfig;
import com.caioamorimr.ordermanagement.security.UserDetailsServiceImpl;
import com.caioamorimr.ordermanagement.services.CategoryService;
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

@WebMvcTest(CategoryResource.class)
@Import(SecurityConfig.class)
class CategoryResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        categoryDTO = new CategoryDTO();
        categoryDTO.setName("Electronics");
    }

    @Test
    @DisplayName("GET /categories should return 200 with paginated list for any authenticated user")
    void findAll_shouldReturn200() throws Exception {
        when(categoryService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(categoryDTO)));

        mockMvc.perform(get("/api/v1/categories").with(asRegularUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /categories/{id} should return 200 when category exists")
    void findById_shouldReturn200_whenCategoryExists() throws Exception {
        when(categoryService.findById(1L)).thenReturn(categoryDTO);

        mockMvc.perform(get("/api/v1/categories/1").with(asRegularUser(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /categories/{id} should return 404 when category does not exist")
    void findById_shouldReturn404_whenCategoryNotFound() throws Exception {
        when(categoryService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/api/v1/categories/99").with(asRegularUser(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /categories should return 201 when caller is admin and payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(categoryService.insert(any(CategoryDTO.class))).thenReturn(categoryDTO);

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /categories should return 403 when caller is not admin")
    void insert_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /categories should return 422 when name is blank")
    void insert_shouldReturn422_whenNameIsBlank() throws Exception {
        categoryDTO.setName("");

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @DisplayName("PUT /categories/{id} should return 200 when caller is admin and payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(categoryService.update(anyLong(), any(CategoryDTO.class))).thenReturn(categoryDTO);

        mockMvc.perform(put("/api/v1/categories/1")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /categories/{id} should return 403 when caller is not admin")
    void update_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/categories/1")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /categories/{id} should return 404 when category does not exist")
    void update_shouldReturn404_whenCategoryNotFound() throws Exception {
        when(categoryService.update(anyLong(), any(CategoryDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/api/v1/categories/99")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 204 when caller is admin and category exists")
    void delete_shouldReturn204_whenCategoryExists() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/v1/categories/1").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 403 when caller is not admin")
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/1").with(csrf()).with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 404 when category does not exist")
    void delete_shouldReturn404_whenCategoryNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(categoryService).delete(99L);

        mockMvc.perform(delete("/api/v1/categories/99").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNotFound());
    }
}