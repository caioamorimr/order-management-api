package com.caioamorimr.ordermanagement.dto;

import com.caioamorimr.ordermanagement.entities.User;

public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;

    public UserDTO() {
    }

    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
