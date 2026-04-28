package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.CategoryDTO;
import com.caioamorimr.ordermanagement.services.CategoryService;
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

@WebMvcTest(CategoryResource.class)
class CategoryResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        categoryDTO = new CategoryDTO();
        categoryDTO.setName("Electronics");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /categories should return 200 with paginated list")
    void findAll_shouldReturn200() throws Exception {
        when(categoryService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(categoryDTO)));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /categories/{id} should return 200 when category exists")
    void findById_shouldReturn200_whenCategoryExists() throws Exception {
        when(categoryService.findById(1L)).thenReturn(categoryDTO);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /categories/{id} should return 404 when category does not exist")
    void findById_shouldReturn404_whenCategoryNotFound() throws Exception {
        when(categoryService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /categories should return 201 when payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(categoryService.insert(any(CategoryDTO.class))).thenReturn(categoryDTO);

        mockMvc.perform(post("/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /categories should return 422 when name is blank")
    void insert_shouldReturn422_whenNameIsBlank() throws Exception {
        categoryDTO.setName("");

        mockMvc.perform(post("/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /categories/{id} should return 200 when payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(categoryService.update(anyLong(), any(CategoryDTO.class))).thenReturn(categoryDTO);

        mockMvc.perform(put("/categories/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /categories/{id} should return 404 when category does not exist")
    void update_shouldReturn404_whenCategoryNotFound() throws Exception {
        when(categoryService.update(anyLong(), any(CategoryDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/categories/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /categories/{id} should return 204 when category exists")
    void delete_shouldReturn204_whenCategoryExists() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/categories/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /categories/{id} should return 404 when category does not exist")
    void delete_shouldReturn404_whenCategoryNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(categoryService).delete(99L);

        mockMvc.perform(delete("/categories/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
