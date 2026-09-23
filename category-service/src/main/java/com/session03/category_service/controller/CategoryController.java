package com.session03.category_service.controller;

import com.session03.category_service.dto.CategoryResponse;
import com.session03.category_service.dto.ProductDto;
import com.session03.category_service.entity.Category;
import com.session03.category_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API demo Feign Client: Lấy thông tin Category và danh sách sản phẩm thuộc category đó từ product-service
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<CategoryResponse> getCategoryWithProducts(@PathVariable Long id) {
        try {
            CategoryResponse response = categoryService.getCategoryWithProducts(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * API gọi trực tiếp Feign Client để lấy danh sách sản phẩm theo categoryId
     */
    @GetMapping("/products-by-category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategoryId(@PathVariable Long categoryId) {
        List<ProductDto> products = categoryService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category saved = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
