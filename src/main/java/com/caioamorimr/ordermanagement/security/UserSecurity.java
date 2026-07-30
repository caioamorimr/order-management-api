package com.caioamorimr.ordermanagement.security;

import org.springframework.stereotype.Component;

/**
 * Resolves ownership rules used by method-level security ({@code @PreAuthorize}) on
 * {@code UserResource}. Kept separate from the controller so the rule is testable in
 * isolation and reusable if other resources ever need "is this me?" checks.
 */
@Component("userSecurity")
public class UserSecurity {

    public boolean isSelf(Long userId, UserPrincipal principal) {
        return principal != null && principal.getId() != null && principal.getId().equals(userId);
    }
}
