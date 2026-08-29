package com.caioamorimr.ordermanagement.services.exceptions;

import java.io.Serial;

public class InvalidRefreshTokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired, or has already been used");
    }
}
