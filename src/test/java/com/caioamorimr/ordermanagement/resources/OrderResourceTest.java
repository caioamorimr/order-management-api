package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderItemInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderUpdateDTO;
import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;
import com.caioamorimr.ordermanagement.services.OrderService;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderResource.class)
class OrderResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDTO orderDTO;
    private OrderInsertDTO insertDTO;
    private OrderUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        orderDTO = new OrderDTO();

        insertDTO = new OrderInsertDTO();
        insertDTO.setMoment(Instant.now());
        insertDTO.setOrderStatus(OrderStatus.WAITING_PAYMENT);
        insertDTO.setClientId(1L);
        OrderItemInsertDTO item = new OrderItemInsertDTO();
        item.setProductId(1L);
        item.setQuantity(2);
        insertDTO.setItems(List.of(item));

        updateDTO = new OrderUpdateDTO();
        updateDTO.setMoment(Instant.now());
        updateDTO.setOrderStatus(OrderStatus.PAID);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /orders should return 200 with paginated list")
    void findAll_shouldReturn200() throws Exception {
        when(orderService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderDTO)));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /orders/{id} should return 200 when order exists")
    void findById_shouldReturn200_whenOrderExists() throws Exception {
        when(orderService.findById(1L)).thenReturn(orderDTO);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /orders/{id} should return 404 when order does not exist")
    void findById_shouldReturn404_whenOrderNotFound() throws Exception {
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /orders should return 201 when payload is valid")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(orderService.insert(any(OrderInsertDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /orders should return 422 when clientId is null")
    void insert_shouldReturn422_whenClientIdIsNull() throws Exception {
        insertDTO.setClientId(null);

        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("clientId"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /orders/{id} should return 200 when payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(orderService.update(anyLong(), any(OrderUpdateDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(put("/orders/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /orders/{id} should return 404 when order does not exist")
    void update_shouldReturn404_whenOrderNotFound() throws Exception {
        when(orderService.update(anyLong(), any(OrderUpdateDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/orders/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /orders/{id} should return 204 when order exists")
    void delete_shouldReturn204_whenOrderExists() throws Exception {
        doNothing().when(orderService).delete(1L);

        mockMvc.perform(delete("/orders/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /orders/{id} should return 404 when order does not exist")
    void delete_shouldReturn404_whenOrderNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(orderService).delete(99L);

        mockMvc.perform(delete("/orders/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
