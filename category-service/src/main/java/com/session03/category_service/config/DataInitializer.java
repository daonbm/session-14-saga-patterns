package com.session03.category_service.config;

import com.session03.category_service.entity.Category;
import com.session03.category_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            log.info("Categories table in 'product-db' is empty. Initializing sample category data...");

            List<Category> initialCategories = List.of(
                    Category.builder()
                            .name("Electronics")
                            .description("Laptops, Desktops, and electronic appliances")
                            .build(),
                    Category.builder()
                            .name("Smartphones")
                            .description("Mobile phones, tablets, and smart devices")
                            .build(),
                    Category.builder()
                            .name("Audio")
                            .description("Headphones, earphones, and speakers")
                            .build(),
                    Category.builder()
                            .name("Accessories")
                            .description("Keyboards, mice, chargers, and cables")
                            .build()
            );

            categoryRepository.saveAll(initialCategories);
            log.info("Successfully seeded {} categories to 'product-db'.", initialCategories.size());
        }
    }
}
