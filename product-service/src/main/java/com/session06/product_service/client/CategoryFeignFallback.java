package com.session06.product_service.client;

import com.session06.product_service.dto.CategoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback class cho CategoryFeignClient.
 * Khi category-service không khả dụng hoặc Circuit Breaker OPEN,
 * các method này sẽ được gọi thay thế → tránh cascading failure.
 */
@Component
@Slf4j
public class CategoryFeignFallback implements CategoryFeignClient {

    @Override
    public CategoryDto getCategoryById(Long id) {
        log.warn("Circuit Breaker ACTIVATED: category-service không khả dụng. Trả về default cho category ID: {}", id);
        return CategoryDto.builder()
                .id(id)
                .name("Unknown Category")
                .description("Category service is currently unavailable")
                .build();
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        log.warn("Circuit Breaker ACTIVATED: category-service không khả dụng. Trả về empty list.");
        return Collections.emptyList();
    }
}
