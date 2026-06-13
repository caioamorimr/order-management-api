package com.caioamorimr.ordermanagement;

import com.caioamorimr.ordermanagement.dto.CategoryDTO;
import com.caioamorimr.ordermanagement.dto.LoginRequest;
import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderItemInsertDTO;
import com.caioamorimr.ordermanagement.dto.ProductDTO;
import com.caioamorimr.ordermanagement.dto.ProductRequestDTO;
import com.caioamorimr.ordermanagement.dto.TokenResponse;
import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void authenticate() throws Exception {
        LoginRequest loginRequest = new LoginRequest("caio@email.com", "123456");

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = objectMapper.readValue(responseBody, TokenResponse.class).token();
    }

    @Test
    @DisplayName("Integration test: create and retrieve category")
    void createAndRetrieveCategory_shouldWork() throws Exception {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setName("Integration Test Category");

        String response = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CategoryDTO created = objectMapper.readValue(response, CategoryDTO.class);
        Long id = created.getId();

        mockMvc.perform(get("/categories/" + id)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Category"));
    }

    @Test
    @DisplayName("Integration test: create user, product, order with items and retrieve")
    void createUserProductOrderAndRetrieve_shouldWork() throws Exception {

        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setName("Integration Test Category");

        String categoryResponse = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readValue(categoryResponse, CategoryDTO.class).getId();

        ProductRequestDTO productDTO = new ProductRequestDTO();
        productDTO.setName("Integration Test Product");
        productDTO.setDescription("Test product description");
        productDTO.setPrice(BigDecimal.valueOf(100.00));
        productDTO.setCategoryIds(Set.of(categoryId));

        String productResponse = mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readValue(productResponse, ProductDTO.class).getId();

        UserInsertDTO userDTO = new UserInsertDTO();
        userDTO.setName("Integration Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setPhone("123456789");
        userDTO.setPassword("password123");

        String userResponse = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readValue(userResponse, UserDTO.class).getId();

        OrderItemInsertDTO item = new OrderItemInsertDTO();
        item.setProductId(productId);
        item.setQuantity(2);

        OrderInsertDTO orderDTO = new OrderInsertDTO();
        orderDTO.setMoment(Instant.now());
        orderDTO.setOrderStatus(OrderStatus.WAITING_PAYMENT);
        orderDTO.setClientId(userId);
        orderDTO.setItems(List.of(item));

        String orderResponse = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readValue(orderResponse, OrderDTO.class).getId();

        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.client.id").value(userId))
                .andExpect(jsonPath("$.items[0].product.id").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }
}