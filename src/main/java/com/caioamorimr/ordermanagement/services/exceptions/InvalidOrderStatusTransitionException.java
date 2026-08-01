package com.caioamorimr.ordermanagement.services.exceptions;

import com.caioamorimr.ordermanagement.entities.enums.OrderStatus;

import java.io.Serial;

public class InvalidOrderStatusTransitionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order status from " + from + " to " + to);
    }
}
