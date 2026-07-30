package com.caioamorimr.ordermanagement.util;

import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.entities.enums.Role;
import com.caioamorimr.ordermanagement.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Authenticates MockMvc requests with our own {@link UserPrincipal}, instead of the
 * generic {@code org.springframework.security.core.userdetails.User} that
 * {@code @WithMockUser} produces.
 * <p>
 * This is required because the {@code @PreAuthorize} ownership expressions used across
 * the resources (e.g. {@code authentication.principal.id}, or bean references like
 * {@code @orderSecurity.isOwner(#id, authentication.principal)}) read properties/types
 * that only exist on {@link UserPrincipal}. {@code @WithMockUser}'s principal doesn't
 * have a {@code getId()}, so it can't be used for these tests.
 */
public final class AuthTestUtils {

    private AuthTestUtils() {
    }

    /**
     * Authenticated principal with ROLE_ADMIN and the given id. Admins bypass every
     * ownership check ({@code hasRole('ADMIN') or ...}), so this is the right default
     * for "happy path" tests that aren't specifically about ownership.
     */
    public static RequestPostProcessor asAdmin(Long id) {
        return asUser(id, Role.ROLE_ADMIN);
    }

    /**
     * Authenticated principal with ROLE_USER and the given id. Use this for tests that
     * specifically exercise ownership rules (self vs. someone else) or that assert a
     * 403 on admin-only endpoints.
     */
    public static RequestPostProcessor asRegularUser(Long id) {
        return asUser(id, Role.ROLE_USER);
    }

    private static RequestPostProcessor asUser(Long id, Role role) {
        User user = new User(id, "Test User " + id, "test-user-" + id + "@email.com", "999999999", "hashed-password", role);
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }
}
