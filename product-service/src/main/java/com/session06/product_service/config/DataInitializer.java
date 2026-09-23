package com.session06.product_service.config;

import com.session06.product_service.entity.Product;
import com.session06.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        List<Product> existingProducts = productRepository.findAll();

        if (existingProducts.isEmpty()) {
            log.info("Database 'product-db' is empty. Initializing sample product data...");
            seedProducts();
        } else {
            // Tự động gán categoryId cho các sản phẩm đã tạo trước đó nếu categoryId đang là null
            boolean needsUpdate = false;
            for (Product p : existingProducts) {
                if (p.getCategoryId() == null) {
                    if ("Electronics".equalsIgnoreCase(p.getCategory())) {
                        p.setCategoryId(1L);
                    } else if ("Smartphones".equalsIgnoreCase(p.getCategory())) {
                        p.setCategoryId(2L);
                    } else if ("Audio".equalsIgnoreCase(p.getCategory())) {
                        p.setCategoryId(3L);
                    } else if ("Accessories".equalsIgnoreCase(p.getCategory())) {
                        p.setCategoryId(4L);
                    } else {
                        p.setCategoryId(1L);
                    }
                    needsUpdate = true;
                }
            }
            if (needsUpdate) {
                productRepository.saveAll(existingProducts);
                log.info("Updated existing products with category IDs in 'product-db'.");
            }
        }
    }

    private void seedProducts() {
        List<Product> initialProducts = List.of(
                Product.builder()
                        .name("Laptop Dell XPS 15")
                        .price(new BigDecimal("1850.00"))
                        .stockQuantity(15)
                        .description("High-performance laptop with 15.6-inch OLED 3.5K display, Intel Core i7")
                        .categoryId(1L)
                        .category("Electronics")
                        .build(),
                Product.builder()
                        .name("iPhone 16 Pro Max 256GB")
                        .price(new BigDecimal("1299.99"))
                        .stockQuantity(30)
                        .description("Apple flagship smartphone with A18 Pro chip, Titanium design")
                        .categoryId(2L)
                        .category("Smartphones")
                        .build(),
                Product.builder()
                        .name("Sony WH-1000XM5 Wireless Headphones")
                        .price(new BigDecimal("399.00"))
                        .stockQuantity(40)
                        .description("Industry-leading noise canceling wireless headphones")
                        .categoryId(3L)
                        .category("Audio")
                        .build(),
                Product.builder()
                        .name("Keychron K2 Mechanical Keyboard")
                        .price(new BigDecimal("89.90"))
                        .stockQuantity(50)
                        .description("Wireless Mechanical Keyboard with Gateron G Pro Brown switches")
                        .categoryId(4L)
                        .category("Accessories")
                        .build(),
                Product.builder()
                        .name("Samsung Galaxy Watch 7")
                        .price(new BigDecimal("299.50"))
                        .stockQuantity(25)
                        .description("Smartwatch with AI health tracking and AMOLED screen")
                        .categoryId(1L)
                        .category("Electronics")
                        .build()
        );

        productRepository.saveAll(initialProducts);
        log.info("Successfully seeded {} products to 'product-db'.", initialProducts.size());
    }
}
