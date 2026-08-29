package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.ProductDTO;
import com.caioamorimr.ordermanagement.dto.ProductRequestDTO;
import com.caioamorimr.ordermanagement.security.JwtUtil;
import com.caioamorimr.ordermanagement.security.SecurityConfig;
import com.caioamorimr.ordermanagement.security.UserDetailsServiceImpl;
import com.caioamorimr.ordermanagement.services.ProductService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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

@WebMvcTest(ProductResource.class)
@Import(SecurityConfig.class)
class ProductResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

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
    @DisplayName("GET /products should return 200 with paginated list for any authenticated user")
    void findAll_shouldReturn200() throws Exception {
        when(productService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(productDTO)));

        mockMvc.perform(get("/api/v1/products").with(asRegularUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /products/{id} should return 200 when product exists")
    void findById_shouldReturn200_whenProductExists() throws Exception {
        when(productService.findById(1L)).thenReturn(productDTO);

        mockMvc.perform(get("/api/v1/products/1").with(asRegularUser(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /products/{id} should return 404 when product does not exist")
    void findById_shouldReturn404_whenProductNotFound() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/api/v1/products/99").with(asRegularUser(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /products should return 201 when caller is admin and payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(productService.insert(any(ProductRequestDTO.class))).thenReturn(productDTO);

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /products should return 403 when caller is not admin")
    void insert_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /products should return 422 when name is blank")
    void insert_shouldReturn422_whenNameIsBlank() throws Exception {
        requestDTO.setName("");

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }

    @Test
    @DisplayName("PUT /products/{id} should return 200 when caller is admin and payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(productService.update(anyLong(), any(ProductRequestDTO.class))).thenReturn(productDTO);

        mockMvc.perform(put("/api/v1/products/1")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /products/{id} should return 403 when caller is not admin")
    void update_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/products/1")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /products/{id} should return 404 when product does not exist")
    void update_shouldReturn404_whenProductNotFound() throws Exception {
        when(productService.update(anyLong(), any(ProductRequestDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/api/v1/products/99")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 204 when caller is admin and product exists")
    void delete_shouldReturn204_whenProductExists() throws Exception {
        doNothing().when(productService).delete(1L);

        mockMvc.perform(delete("/api/v1/products/1").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 403 when caller is not admin")
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1").with(csrf()).with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 404 when product does not exist")
    void delete_shouldReturn404_whenProductNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /products/{productId}/categories/{categoryId} should return 200 when caller is admin")
    void addCategory_shouldReturn200() throws Exception {
        when(productService.addCategory(1L, 2L)).thenReturn(productDTO);

        mockMvc.perform(put("/api/v1/products/1/categories/2").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /products/{productId}/categories/{categoryId} should return 403 when caller is not admin")
    void addCategory_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/products/1/categories/2").with(csrf()).with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /products/{productId}/categories/{categoryId} should return 204 when caller is admin")
    void removeCategory_shouldReturn204() throws Exception {
        doNothing().when(productService).removeCategory(1L, 2L);

        mockMvc.perform(delete("/api/v1/products/1/categories/2").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /products/{productId}/categories/{categoryId} should return 403 when caller is not admin")
    void removeCategory_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1/categories/2").with(csrf()).with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }
}