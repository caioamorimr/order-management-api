package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.ProductDTO;
import com.caioamorimr.ordermanagement.dto.ProductRequestDTO;
import com.caioamorimr.ordermanagement.services.ProductService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductResource.class)
class ProductResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDTO productDTO;
    private ProductRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        productDTO = new ProductDTO();
        productDTO.setName("Laptop");
        productDTO.setDescription("Gaming laptop");
        productDTO.setPrice(BigDecimal.valueOf(1500.00));
        productDTO.setImgUrl("http://example.com/laptop.jpg");
        productDTO.setCategoryIds(Set.of(1L));

        requestDTO = new ProductRequestDTO();
        requestDTO.setName("Laptop");
        requestDTO.setDescription("Gaming laptop");
        requestDTO.setPrice(BigDecimal.valueOf(1500.00));
        requestDTO.setImgUrl("http://example.com/laptop.jpg");
        requestDTO.setCategoryIds(Set.of(1L));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products should return 200 with paginated list")
    void findAll_shouldReturn200() throws Exception {
        when(productService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(productDTO)));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/{id} should return 200 when product exists")
    void findById_shouldReturn200_whenProductExists() throws Exception {
        when(productService.findById(1L)).thenReturn(productDTO);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/{id} should return 404 when product does not exist")
    void findById_shouldReturn404_whenProductNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /products should return 201 when payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(productService.insert(any(ProductRequestDTO.class))).thenReturn(productDTO);

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /products should return 422 when name is blank")
    void insert_shouldReturn422_whenNameIsBlank() throws Exception {
        requestDTO.setName("");

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /products/{id} should return 200 when payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(productService.update(anyLong(), any(ProductRequestDTO.class))).thenReturn(productDTO);

        mockMvc.perform(put("/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /products/{id} should return 404 when product does not exist")
    void update_shouldReturn404_whenProductNotFound() throws Exception {
        when(productService.update(anyLong(), any(ProductRequestDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/products/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /products/{id} should return 204 when product exists")
    void delete_shouldReturn204_whenProductExists() throws Exception {
        doNothing().when(productService).delete(1L);

        mockMvc.perform(delete("/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /products/{id} should return 404 when product does not exist")
    void delete_shouldReturn404_whenProductNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(productService).delete(99L);

        mockMvc.perform(delete("/products/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /products/{productId}/categories/{categoryId} should return 200")
    void addCategory_shouldReturn200() throws Exception {
        when(productService.addCategory(1L, 2L)).thenReturn(productDTO);

        mockMvc.perform(put("/products/1/categories/2").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /products/{productId}/categories/{categoryId} should return 204")
    void removeCategory_shouldReturn204() throws Exception {
        doNothing().when(productService).removeCategory(1L, 2L);

        mockMvc.perform(delete("/products/1/categories/2").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
