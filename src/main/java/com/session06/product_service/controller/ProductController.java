package com.session06.product_service.controller;

import com.session06.product_service.client.CategoryFeignClient;
import com.session06.product_service.dto.CategoryDto;
import com.session06.product_service.dto.ProductDetailResponse;
import com.session06.product_service.entity.Product;
import com.session06.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryFeignClient categoryFeignClient;

    @Value("${server.port}")
    private String serverPort;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategoryId(@PathVariable Long categoryId) {
        List<Product> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API demo Feign Client từ Product Service sang Category Service: Lấy chi tiết sản phẩm kèm Category Info
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(prod -> {
                    CategoryDto catDto = null;
                    if (prod.getCategoryId() != null) {
                        try {
                            catDto = categoryFeignClient.getCategoryById(prod.getCategoryId());
                        } catch (Exception e) {
                            // Category service fallback
                        }
                    }
                    ProductDetailResponse res = com.session06.product_service.dto.ProductDetailResponse.builder()
                            .id(prod.getId())
                            .name(prod.getName())
                            .price(prod.getPrice())
                            .stockQuantity(prod.getStockQuantity())
                            .description(prod.getDescription())
                            .categoryId(prod.getCategoryId())
                            .categoryInfo(catDto)
                            .message("Product details with Category enriched via Feign Client from Category Service")
                            .build();
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @GetMapping("/port-info")
    public ResponseEntity<String> getPortInfo() {
        return ResponseEntity.ok("Product Service running on port: " + serverPort);
    }
}
