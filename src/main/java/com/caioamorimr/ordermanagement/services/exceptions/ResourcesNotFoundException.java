package com.caioamorimr.ordermanagement.services.exceptions;

import java.io.Serial;
import java.util.Set;

public class ResourcesNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ResourcesNotFoundException(Set<Long> ids) {
        super("Resources not found. Ids " + ids);
    }
}
