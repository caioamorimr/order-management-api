package com.caioamorimr.ordermanagement.dto;

import com.caioamorimr.ordermanagement.entities.Order;
import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderDTO {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private Instant moment;

    private OrderStatus orderStatus;
    private UserDTO client;
    private PaymentDTO payment;
    private Set<OrderItemDTO> items;
    private BigDecimal total;

    public OrderDTO() {
    }

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.moment = order.getMoment();
        this.orderStatus = order.getOrderStatus();
        this.client = new UserDTO(order.getClient());
        this.payment = order.getPayment() != null ? new PaymentDTO(order.getPayment()) : null;
        this.items = order.getItems().stream()
                .map(OrderItemDTO::new)
                .collect(Collectors.toSet());
        this.total = order.getTotal();
    }

    public Long getId() {
        return id;
    }

    public Instant getMoment() {
        return moment;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public UserDTO getClient() {
        return client;
    }

    public PaymentDTO getPayment() {
        return payment;
    }

    public Set<OrderItemDTO> getItems() {
        return items;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
