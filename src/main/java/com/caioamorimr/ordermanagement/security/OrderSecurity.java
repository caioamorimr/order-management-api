package com.caioamorimr.ordermanagement.security;

import com.caioamorimr.ordermanagement.repositories.OrderRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves ownership rules used by method-level security ({@code @PreAuthorize}) on
 * {@code OrderResource}. A regular user may only reach an order that belongs to them;
 * an admin bypasses this check entirely (see the {@code hasRole('ADMIN') or ...} expressions).
 */
@Component("orderSecurity")
public class OrderSecurity {

    private final OrderRepository orderRepository;

    public OrderSecurity(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean isOwner(Long orderId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return false;
        }
        return orderRepository.findById(orderId)
                .map(order -> order.getClient() != null && principal.getId().equals(order.getClient().getId()))
                .orElse(false);
    }
}
