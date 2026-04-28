package com.caioamorimr.ordermanagement;

import com.caioamorimr.ordermanagement.dto.CategoryDTO;
import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderItemInsertDTO;
import com.caioamorimr.ordermanagement.dto.ProductDTO;
import com.caioamorimr.ordermanagement.dto.ProductRequestDTO;
import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integration test: create and retrieve category")
    void createAndRetrieveCategory_shouldWork() throws Exception {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setName("Integration Test Category");

        // Create category
        String response = mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CategoryDTO created = objectMapper.readValue(response, CategoryDTO.class);
        Long id = created.getId();

        // Retrieve category
        mockMvc.perform(get("/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Category"));
    }

    @Test
    @DisplayName("Integration test: create user, product, order with items and retrieve")
    void createUserProductOrderAndRetrieve_shouldWork() throws Exception {
        // Create category
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setName("Integration Test Category");
        String categoryResponse = mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        CategoryDTO createdCategory = objectMapper.readValue(categoryResponse, CategoryDTO.class);
        Long categoryId = createdCategory.getId();

        // Create product
        ProductRequestDTO productDTO = new ProductRequestDTO();
        productDTO.setName("Integration Test Product");
        productDTO.setDescription("Test product description");
        productDTO.setPrice(BigDecimal.valueOf(100.00));
        productDTO.setCategoryIds(Set.of(categoryId));
        String productResponse = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ProductDTO createdProduct = objectMapper.readValue(productResponse, ProductDTO.class);
        Long productId = createdProduct.getId();

        // Create user
        UserInsertDTO userDTO = new UserInsertDTO();
        userDTO.setName("Integration Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setPhone("123456789");
        userDTO.setPassword("password123");
        String userResponse = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UserDTO createdUser = objectMapper.readValue(userResponse, UserDTO.class);
        Long userId = createdUser.getId();

        // Create order
        OrderInsertDTO orderDTO = new OrderInsertDTO();
        orderDTO.setMoment(Instant.now());
        orderDTO.setOrderStatus(OrderStatus.WAITING_PAYMENT);
        orderDTO.setClientId(userId);
        OrderItemInsertDTO item = new OrderItemInsertDTO();
        item.setProductId(productId);
        item.setQuantity(2);
        orderDTO.setItems(List.of(item));
        String orderResponse = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        OrderDTO createdOrder = objectMapper.readValue(orderResponse, OrderDTO.class);
        Long orderId = createdOrder.getId();

        // Retrieve order
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.client.id").value(userId))
                .andExpect(jsonPath("$.items[0].product.id").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }
}
