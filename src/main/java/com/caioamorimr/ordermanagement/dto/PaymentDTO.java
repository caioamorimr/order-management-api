package com.caioamorimr.ordermanagement.dto;

import com.caioamorimr.ordermanagement.entities.Payment;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class PaymentDTO {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private Instant moment;

    public PaymentDTO() {
    }

    public PaymentDTO(Payment payment) {
        this.id = payment.getId();
        this.moment = payment.getMoment();
    }

    public Long getId() {
        return id;
    }

    public Instant getMoment() {
        return moment;
    }
}
