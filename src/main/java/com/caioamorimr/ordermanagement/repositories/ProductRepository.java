package com.caioamorimr.ordermanagement.repositories;

import com.caioamorimr.ordermanagement.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
