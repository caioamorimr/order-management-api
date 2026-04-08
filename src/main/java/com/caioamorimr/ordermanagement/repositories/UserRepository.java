package com.caioamorimr.ordermanagement.repositories;

import com.caioamorimr.ordermanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
