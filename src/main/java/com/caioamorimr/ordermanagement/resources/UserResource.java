package com.caioamorimr.ordermanagement.resources;

import com.caioamorimr.ordermanagement.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/users")
public class UserResource {

    @GetMapping(value = "/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        User user = new User(1L, "Caio", "caio@email.com", "123456789", "caio123");
        return ResponseEntity.ok(user);
    }
}
