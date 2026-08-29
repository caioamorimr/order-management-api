package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderItemInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderUpdateDTO;
import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;
import com.caioamorimr.ordermanagement.security.JwtUtil;
import com.caioamorimr.ordermanagement.security.OrderSecurity;
import com.caioamorimr.ordermanagement.security.SecurityConfig;
import com.caioamorimr.ordermanagement.security.UserDetailsServiceImpl;
import com.caioamorimr.ordermanagement.services.OrderService;
import com.caioamorimr.ordermanagement.services.exceptions.InvalidOrderStatusTransitionException;
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

import java.time.Instant;
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

@WebMvcTest(OrderResource.class)
@Import(SecurityConfig.class)
class OrderResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean(name = "orderSecurity")
    private OrderSecurity orderSecurity;

    private OrderDTO orderDTO;
    private OrderInsertDTO insertDTO;
    private OrderUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        orderDTO = new OrderDTO();

        insertDTO = new OrderInsertDTO();
        insertDTO.setMoment(Instant.now());
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
    @DisplayName("GET /orders should return 200 with paginated list when caller is admin")
    void findAll_shouldReturn200_whenAdmin() throws Exception {
        when(orderService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderDTO)));

        mockMvc.perform(get("/api/v1/orders").with(asAdmin(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /orders should return 403 when caller is not admin")
    void findAll_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /orders/{id} should return 200 when caller is admin")
    void findById_shouldReturn200_whenOrderExists() throws Exception {
        when(orderService.findById(1L)).thenReturn(orderDTO);

        mockMvc.perform(get("/api/v1/orders/1").with(asAdmin(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /orders/{id} should return 200 when caller owns the order")
    void findById_shouldReturn200_whenOwner() throws Exception {
        when(orderService.findById(1L)).thenReturn(orderDTO);
        when(orderSecurity.isOwner(eq(1L), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/1").with(asRegularUser(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /orders/{id} should return 403 when caller does not own the order")
    void findById_shouldReturn403_whenNotOwnerAndNotAdmin() throws Exception {
        when(orderSecurity.isOwner(eq(1L), any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/orders/1").with(asRegularUser(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /orders/{id} should return 404 when order does not exist")
    void findById_shouldReturn404_whenOrderNotFound() throws Exception {
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(get("/api/v1/orders/99").with(asAdmin(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /orders should return 201 when a regular user places an order for themselves")
    void insert_shouldReturn201_whenPayloadIsValid() throws Exception {
        when(orderService.insert(any(OrderInsertDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /orders should return 403 when a regular user tries to place an order for someone else")
    void insert_shouldReturn403_whenClientIdDoesNotMatchCaller() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .with(asRegularUser(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /orders should return 201 when an admin places an order on behalf of another client")
    void insert_shouldReturn201_whenAdminPlacesOrderForAnotherClient() throws Exception {
        when(orderService.insert(any(OrderInsertDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .with(asAdmin(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /orders should return 422 when clientId is null")
    void insert_shouldReturn422_whenClientIdIsNull() throws Exception {
        insertDTO.setClientId(null);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insertDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].fieldName").value("clientId"));
    }

    @Test
    @DisplayName("PUT /orders/{id} should return 200 when caller is admin and payload is valid")
    void update_shouldReturn200_whenPayloadIsValid() throws Exception {
        when(orderService.update(anyLong(), any(OrderUpdateDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(put("/api/v1/orders/1")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /orders/{id} should return 403 when caller is not admin, even if they own the order")
    void update_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/orders/1")
                        .with(csrf())
                        .with(asRegularUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /orders/{id} should return 404 when order does not exist")
    void update_shouldReturn404_whenOrderNotFound() throws Exception {
        when(orderService.update(anyLong(), any(OrderUpdateDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        mockMvc.perform(put("/api/v1/orders/99")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /orders/{id} should return 409 when the requested status transition is illegal")
    void update_shouldReturn409_whenTransitionIsInvalid() throws Exception {
        when(orderService.update(anyLong(), any(OrderUpdateDTO.class)))
                .thenThrow(new InvalidOrderStatusTransitionException(OrderStatus.WAITING_PAYMENT, OrderStatus.DELIVERED));

        mockMvc.perform(put("/api/v1/orders/1")
                        .with(csrf())
                        .with(asAdmin(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Invalid Order Status Transition"));
    }

    @Test
    @DisplayName("DELETE /orders/{id} should return 204 when caller is admin and order exists")
    void delete_shouldReturn204_whenOrderExists() throws Exception {
        doNothing().when(orderService).delete(1L);

        mockMvc.perform(delete("/api/v1/orders/1").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /orders/{id} should return 403 when caller is not admin")
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/1").with(csrf()).with(asRegularUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /orders/{id} should return 404 when order does not exist")
    void delete_shouldReturn404_whenOrderNotFound() throws Exception {
        doThrow(new ResourceNotFoundException(99L)).when(orderService).delete(99L);

        mockMvc.perform(delete("/api/v1/orders/99").with(csrf()).with(asAdmin(1L)))
                .andExpect(status().isNotFound());
    }
}