package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderItemInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderUpdateDTO;
import com.caioamorimr.ordermanagement.entities.Order;
import com.caioamorimr.ordermanagement.entities.Product;
import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;
import com.caioamorimr.ordermanagement.repositories.OrderRepository;
import com.caioamorimr.ordermanagement.repositories.ProductRepository;
import com.caioamorimr.ordermanagement.repositories.UserRepository;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private Order order;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User(1L, "Caio", "caio@email.com", "988888888", "hashed_password");
        product = new Product(1L, "Laptop", "Gaming laptop", BigDecimal.valueOf(1500.00), "http://example.com/laptop.jpg");
        order = new Order(1L, Instant.now(), OrderStatus.WAITING_PAYMENT, user);
    }

    @Test
    @DisplayName("findAll should return a paginated page of OrderDTOs")
    void findAll_shouldReturnPageOfOrderDTOs() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));

        Page<OrderDTO> result = orderService.findAll(pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        verify(orderRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("findById should return OrderDTO when order exists")
    void findById_shouldReturnOrderDTO_whenOrderExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when order does not exist")
    void findById_shouldThrowResourceNotFoundException_whenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("insert should persist order and return OrderDTO")
    void insert_shouldPersistOrderAndReturnOrderDTO() {
        OrderInsertDTO dto = new OrderInsertDTO();
        dto.setMoment(Instant.now());
        dto.setOrderStatus(OrderStatus.WAITING_PAYMENT);
        dto.setClientId(1L);
        OrderItemInsertDTO itemDto = new OrderItemInsertDTO();
        itemDto.setProductId(1L);
        itemDto.setQuantity(2);
        dto.setItems(List.of(itemDto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO result = orderService.insert(dto);

        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when order does not exist")
    void delete_shouldThrowResourceNotFoundException_whenOrderNotFound() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when order does not exist")
    void update_shouldThrowResourceNotFoundException_whenOrderNotFound() {
        when(orderRepository.getReferenceById(99L)).thenThrow(EntityNotFoundException.class);

        OrderUpdateDTO dto = new OrderUpdateDTO();
        dto.setMoment(Instant.now());
        dto.setOrderStatus(OrderStatus.PAID);

        assertThatThrownBy(() -> orderService.update(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update should update order when exists")
    void update_shouldUpdateOrder_whenExists() {
        OrderUpdateDTO dto = new OrderUpdateDTO();
        dto.setMoment(Instant.now());
        dto.setOrderStatus(OrderStatus.PAID);

        when(orderRepository.getReferenceById(1L)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO result = orderService.update(1L, dto);

        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).save(order);
    }
}
