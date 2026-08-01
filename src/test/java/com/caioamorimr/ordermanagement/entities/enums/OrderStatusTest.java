package com.caioamorimr.ordermanagement.entities.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} should be allowed")
    @CsvSource({
            "WAITING_PAYMENT, PAID",
            "WAITING_PAYMENT, CANCELED",
            "PAID, SHIPPED",
            "PAID, CANCELED",
            "SHIPPED, DELIVERED"
    })
    @DisplayName("canTransitionTo should allow every documented lifecycle transition")
    void canTransitionTo_shouldAllowDocumentedTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} should be rejected")
    @CsvSource({
            "WAITING_PAYMENT, SHIPPED",
            "WAITING_PAYMENT, DELIVERED",
            "PAID, DELIVERED",
            "PAID, WAITING_PAYMENT",
            "SHIPPED, WAITING_PAYMENT",
            "SHIPPED, PAID",
            "SHIPPED, CANCELED",
            "DELIVERED, WAITING_PAYMENT",
            "DELIVERED, PAID",
            "DELIVERED, SHIPPED",
            "DELIVERED, CANCELED",
            "CANCELED, WAITING_PAYMENT",
            "CANCELED, PAID",
            "CANCELED, SHIPPED",
            "CANCELED, DELIVERED"
    })
    @DisplayName("canTransitionTo should reject transitions that skip a step, go backwards, or leave a terminal state")
    void canTransitionTo_shouldRejectIllegalTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("canTransitionTo should always allow a status to transition to itself (no-op)")
    void canTransitionTo_shouldAllowSameStatus(OrderStatus status) {
        assertThat(status.canTransitionTo(status)).isTrue();
    }

    @Test
    @DisplayName("DELIVERED and CANCELED are terminal: no outgoing transitions to any other status")
    void terminalStatuses_shouldHaveNoOutgoingTransitions() {
        for (OrderStatus target : OrderStatus.values()) {
            if (target != OrderStatus.DELIVERED) {
                assertThat(OrderStatus.DELIVERED.canTransitionTo(target)).isFalse();
            }
            if (target != OrderStatus.CANCELED) {
                assertThat(OrderStatus.CANCELED.canTransitionTo(target)).isFalse();
            }
        }
    }
}