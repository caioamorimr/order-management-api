package com.caioamorimr.ordermanagement.repositories;

import com.caioamorimr.ordermanagement.entities.OrderItem;
import com.caioamorimr.ordermanagement.entities.pk.OrderItemPK;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemPK> {
}
