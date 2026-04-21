package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.entities.Product;
import com.caioamorimr.ordermanagement.repositories.ProductRepository;
import com.caioamorimr.ordermanagement.services.exceptions.DatabaseException;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Product insert(Product product) {
        return productRepository.save(product);
    }

    public void delete(Long id) {
        if(!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        try {
            productRepository.deleteById(id);
        }
        catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public Product update(Long id, Product product) {
        try {
            Product entity = productRepository.getReferenceById(id);
            updateData(entity, product);
            return productRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(Product entity, Product product) {
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setImgUrl(product.getImgUrl());
    }
}
