package com.caioamorimr.ordermanagement.dto;

public record TokenResponse(String token, String type) {

    public TokenResponse(String token) {
        this(token, "Bearer");
    }
}