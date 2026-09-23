package com.session03.category_service.service;

import com.session03.category_service.dto.CategoryResponse;
import com.session03.category_service.dto.ProductDto;
import com.session03.category_service.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories();
    Optional<Category> getCategoryById(Long id);
    Category createCategory(Category category);
    CategoryResponse getCategoryWithProducts(Long id);
    List<ProductDto> getProductsByCategoryId(Long categoryId);
}
