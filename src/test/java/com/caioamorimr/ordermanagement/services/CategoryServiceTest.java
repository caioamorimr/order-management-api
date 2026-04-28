package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.dto.CategoryDTO;
import com.caioamorimr.ordermanagement.entities.Category;
import com.caioamorimr.ordermanagement.repositories.CategoryRepository;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Electronics");
    }

    @Test
    @DisplayName("findAll should return a paginated page of CategoryDTOs")
    void findAll_shouldReturnPageOfCategoryDTOs() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(categoryRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(category)));

        Page<CategoryDTO> result = categoryService.findAll(pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Electronics");
        verify(categoryRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("findById should return CategoryDTO when category exists")
    void findById_shouldReturnCategoryDTO_whenCategoryExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryDTO result = categoryService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when category does not exist")
    void findById_shouldThrowResourceNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("insert should persist category and return CategoryDTO")
    void insert_shouldPersistCategoryAndReturnCategoryDTO() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Books");

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDTO result = categoryService.insert(dto);

        assertThat(result.getName()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when category does not exist")
    void delete_shouldThrowResourceNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when category does not exist")
    void update_shouldThrowResourceNotFoundException_whenCategoryNotFound() {
        when(categoryRepository.getReferenceById(99L)).thenThrow(EntityNotFoundException.class);

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Updated");

        assertThatThrownBy(() -> categoryService.update(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update should update category when exists")
    void update_shouldUpdateCategory_whenExists() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Updated Electronics");

        when(categoryRepository.getReferenceById(1L)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDTO result = categoryService.update(1L, dto);

        assertThat(result.getName()).isEqualTo("Updated Electronics");
        verify(categoryRepository).save(category);
    }
}
