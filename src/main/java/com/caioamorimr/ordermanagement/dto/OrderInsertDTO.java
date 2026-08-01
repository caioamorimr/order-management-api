package com.caioamorimr.ordermanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderInsertDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private Instant moment;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemInsertDTO> items = new ArrayList<>();

    @NotNull(message = "Client ID is required")
    private Long clientId;

    public OrderInsertDTO() {
    }

    public Instant getMoment() {
        return moment;
    }

    public void setMoment(Instant moment) {
        this.moment = moment;
    }

    public List<OrderItemInsertDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemInsertDTO> items) {
        this.items = items;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}