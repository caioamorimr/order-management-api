package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.UserDTO;
import com.caioamorimr.ordermanagement.dto.UserInsertDTO;
import com.caioamorimr.ordermanagement.dto.UserUpdateDTO;
import com.caioamorimr.ordermanagement.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(value = "/users")
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * Listing every user in the system is an administrative action.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    /**
     * An admin can look up any user; a regular user can only look up themselves.
     */
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication.principal)")
    @GetMapping(value = "/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * User creation through this endpoint is an administrative action (bulk/manual
     * account creation). Public self-registration is a separate concern — see
     * the upcoming {@code POST /auth/register} endpoint — and intentionally not
     * handled here.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserDTO> insert(@Valid @RequestBody UserInsertDTO dto) {
        UserDTO created = userService.insert(dto);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(uri).body(created);
    }

    /**
     * Deleting a user is kept admin-only for now. Self-service account deletion is
     * a reasonable future addition, but it deserves its own review (e.g. what happens
     * to that user's existing orders) instead of being bundled into this fix.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * An admin can update any user; a regular user can only update their own profile.
     */
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication.principal)")
    @PutMapping(value = "/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }
}
