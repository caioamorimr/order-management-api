package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.entities.Category;
import com.caioamorimr.ordermanagement.repositories.CategoryRepository;
import com.caioamorimr.ordermanagement.services.exceptions.DatabaseException;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id){
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Category insert(Category category) {
        return categoryRepository.save(category);
    }

    public void delete(Long id){
        if(!categoryRepository.existsById(id)){
            throw new ResourceNotFoundException(id);
        }
        try {
            categoryRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public Category update(Long id, Category category) {
        try {
            Category entity = categoryRepository.getReferenceById(id);
            entity.setName(category.getName());
            return categoryRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }
}
