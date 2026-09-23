package com.session06.product_service.client;

import com.session06.product_service.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "category-service", fallback = CategoryFeignFallback.class)
public interface CategoryFeignClient {

    @GetMapping("/api/categories/{id}")
    CategoryDto getCategoryById(@PathVariable("id") Long id);

    @GetMapping("/api/categories")
    List<CategoryDto> getAllCategories();
}
