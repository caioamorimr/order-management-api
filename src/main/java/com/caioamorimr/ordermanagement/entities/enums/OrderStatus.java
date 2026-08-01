package com.caioamorimr.ordermanagement.entities.enums;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {

    WAITING_PAYMENT(1),
    PAID(2),
    SHIPPED(3),
    DELIVERED(4),
    CANCELED(5);

    private final int code;

    private OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static OrderStatus valueOf(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("Order status with code %d does not exist", code));
    }

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            WAITING_PAYMENT, Set.of(PAID, CANCELED),
            PAID, Set.of(SHIPPED, CANCELED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELED, Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return this == target || ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
