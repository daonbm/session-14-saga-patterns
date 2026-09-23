package com.session03.category_service.service;

import com.session03.category_service.client.ProductFeignClient;
import com.session03.category_service.dto.CategoryResponse;
import com.session03.category_service.dto.ProductDto;
import com.session03.category_service.entity.Category;
import com.session03.category_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductFeignClient productFeignClient;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public CategoryResponse getCategoryWithProducts(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));

        log.info("Calling product-service using Feign Client to get products for category ID: {}", id);
        List<ProductDto> products;
        String statusMessage;
        try {
            products = productFeignClient.getProductsByCategoryId(id);
            if (products == null) {
                products = Collections.emptyList();
            }
            statusMessage = "Fetched category info from MySQL and " + products.size() + " products via Feign Client from Product Service";
        } catch (Exception e) {
            log.error("Failed to fetch products from product-service via Feign: {}", e.getMessage());
            products = Collections.emptyList();
            statusMessage = "Category info retrieved, but failed to reach Product Service via Feign Client: " + e.getMessage();
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .products(products)
                .message(statusMessage)
                .build();
    }

    @Override
    public List<ProductDto> getProductsByCategoryId(Long categoryId) {
        log.info("Directly calling Product Service via Feign Client for category ID: {}", categoryId);
        return productFeignClient.getProductsByCategoryId(categoryId);
    }
}
