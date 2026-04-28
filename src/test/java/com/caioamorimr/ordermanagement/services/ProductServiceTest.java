package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.dto.ProductDTO;
import com.caioamorimr.ordermanagement.dto.ProductRequestDTO;
import com.caioamorimr.ordermanagement.entities.Category;
import com.caioamorimr.ordermanagement.entities.Product;
import com.caioamorimr.ordermanagement.repositories.CategoryRepository;
import com.caioamorimr.ordermanagement.repositories.ProductRepository;
import com.caioamorimr.ordermanagement.services.exceptions.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Electronics");
        product = new Product(1L, "Laptop", "Gaming laptop", BigDecimal.valueOf(1500.00), "http://example.com/laptop.jpg");
        product.addCategory(category);
    }

    @Test
    @DisplayName("findAll should return a paginated page of ProductDTOs")
    void findAll_shouldReturnPageOfProductDTOs() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductDTO> result = productService.findAll(pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("findById should return ProductDTO when product exists")
    void findById_shouldReturnProductDTO_whenProductExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDTO result = productService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when product does not exist")
    void findById_shouldThrowResourceNotFoundException_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("insert should persist product and return ProductDTO")
    void insert_shouldPersistProductAndReturnProductDTO() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Laptop");
        dto.setDescription("Gaming laptop");
        dto.setPrice(BigDecimal.valueOf(1500.00));
        dto.setImgUrl("http://example.com/laptop.jpg");
        dto.setCategoryIds(Set.of(1L));

        when(categoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.insert(dto);

        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when product does not exist")
    void delete_shouldThrowResourceNotFoundException_whenProductNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("update should update product when exists")
    void update_shouldUpdateProduct_whenExists() {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Updated Laptop");
        dto.setDescription("Updated description");
        dto.setPrice(BigDecimal.valueOf(1600.00));
        dto.setImgUrl("http://example.com/updated.jpg");
        dto.setCategoryIds(Set.of(1L));

        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(categoryRepository.findAllById(Set.of(1L))).thenReturn(List.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.update(1L, dto);

        assertThat(result.getName()).isEqualTo("Updated Laptop");
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("addCategory should add category to product")
    void addCategory_shouldAddCategoryToProduct() {
        Category newCategory = new Category(2L, "Gaming");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO result = productService.addCategory(1L, 2L);

        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("removeCategory should remove category from product")
    void removeCategory_shouldRemoveCategoryFromProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.removeCategory(1L, 1L);

        verify(productRepository).save(product);
    }
}
