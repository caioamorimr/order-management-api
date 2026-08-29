package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.dto.OrderDTO;
import com.caioamorimr.ordermanagement.dto.OrderInsertDTO;
import com.caioamorimr.ordermanagement.dto.OrderUpdateDTO;
import com.caioamorimr.ordermanagement.services.OrderService;
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
@RequestMapping(value = "/api/v1/orders")
public class OrderResource {

    private final OrderService orderService;

    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Browsing every order in the system is an administrative action.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    /**
     * An admin can look up any order; a regular user can only look up their own.
     */
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#id, authentication.principal)")
    @GetMapping(value = "/{id}")
    public ResponseEntity<OrderDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /**
     * A regular user may only place an order for themselves, #dto.clientId must match
     * their own id. An admin may place an order on behalf of any client (e.g. phone/counter
     * orders). This is evaluated against the already-bound and already-validated DTO.
     */
    @PreAuthorize("hasRole('ADMIN') or #dto.clientId == authentication.principal.id")
    @PostMapping
    public ResponseEntity<OrderDTO> insert(@Valid @RequestBody OrderInsertDTO dto) {
        OrderDTO created = orderService.insert(dto);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(uri).body(created);
    }

    /**
     * Deleting an order is kept admin-only — it's a destructive, administrative action,
     * distinct from a customer-facing "cancel" (which would be a status transition, not
     * a row deletion).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Changing an order's status/moment is a fulfillment action (payment confirmation,
     * shipping, etc.) and is kept admin-only. A customer unilaterally marking their own
     * order as PAID or DELIVERED would defeat the purpose of the order lifecycle.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}")
    public ResponseEntity<OrderDTO> update(@PathVariable Long id, @Valid @RequestBody OrderUpdateDTO dto) {
        return ResponseEntity.ok(orderService.update(id, dto));
    }
}
