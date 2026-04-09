package com.caioamorimr.ordermanagement.repositories;

import com.caioamorimr.ordermanagement.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
