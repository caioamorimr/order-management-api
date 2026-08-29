package com.caioamorimr.ordermanagement.dto;

public record TokenResponse(String token, String type, String refreshToken) {

    public TokenResponse(String token, String refreshToken) {
        this(token, "Bearer", refreshToken);
    }
}