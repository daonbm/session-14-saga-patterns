package com.session03.category_service.client;

import com.session03.category_service.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    /**
     * Gọi endpoint /api/products/category/{categoryId} bên product-service bằng Feign Client
     */
    @GetMapping("/api/products/category/{categoryId}")
    List<ProductDto> getProductsByCategoryId(@PathVariable("categoryId") Long categoryId);

    /**
     * Gọi endpoint /api/products bên product-service bằng Feign Client
     */
    @GetMapping("/api/products")
    List<ProductDto> getAllProducts(@RequestParam(value = "categoryId", required = false) Long categoryId);
}
