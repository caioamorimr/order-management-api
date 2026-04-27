package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.dto.ProductDTO;
import com.caioamorimr.ordermanagement.dto.ProductRequestDTO;
import com.caioamorimr.ordermanagement.entities.Category;
import com.caioamorimr.ordermanagement.entities.Product;
import com.caioamorimr.ordermanagement.repositories.CategoryRepository;
import com.caioamorimr.ordermanagement.repositories.ProductRepository;
import com.caioamorimr.ordermanagement.services.exceptions.DatabaseException;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import com.caioamorimr.ordermanagement.services.exceptions.ResourcesNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductDTO::new);
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return new ProductDTO(product);
    }

    @Transactional
    public ProductDTO insert(ProductRequestDTO dto) {
        Product product = new Product();
        applyDtoToEntity(product, dto);
        product = productRepository.save(product);
        return new ProductDTO(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        try {
            productRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Transactional
    public ProductDTO update(Long id, ProductRequestDTO dto) {
        try {
            Product entity = productRepository.getReferenceById(id);
            applyDtoToEntity(entity, dto);
            entity = productRepository.save(entity);
            return new ProductDTO(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    @Transactional
    public ProductDTO addCategory(Long productId, Long categoryId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(productId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(categoryId));
        product.addCategory(category);
        return new ProductDTO(productRepository.save(product));
    }

    @Transactional
    public void removeCategory(Long productId, Long categoryId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(productId));
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(categoryId);
        }

        product.removeCategory(categoryId);
        productRepository.save(product);
    }

    private void applyDtoToEntity(Product entity, ProductRequestDTO dto) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setPrice(dto.getPrice());
        entity.setImgUrl(dto.getImgUrl());

        entity.clearCategories();
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(dto.getCategoryIds()));

            if (categories.size() != dto.getCategoryIds().size()) {
                Set<Long> foundIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
                Set<Long> missingIds = new HashSet<>(dto.getCategoryIds());
                missingIds.removeAll(foundIds);
                throw new ResourcesNotFoundException(missingIds);
            }
            categories.forEach(entity::addCategory);
        }
    }
}
