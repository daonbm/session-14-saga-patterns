package com.session06.product_service.service;

import com.session06.product_service.dto.StockOperationResponse;
import com.session06.product_service.dto.StockReservationItem;
import com.session06.product_service.entity.Product;
import com.session06.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        existingProduct.setName(productDetails.getName());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setStockQuantity(productDetails.getStockQuantity());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setCategoryId(productDetails.getCategoryId());
        existingProduct.setCategory(productDetails.getCategory());

        return productRepository.save(existingProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public StockOperationResponse reserveStock(List<StockReservationItem> items) {
        log.info("[PRODUCT-SERVICE] [SAGA STEP: RESERVE STOCK] Bắt đầu kiểm tra và giữ kho cho {} mặt hàng", items.size());

        // Pha 1: Kiểm tra tồn kho trước
        for (StockReservationItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + item.getProductId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                String errMsg = String.format("Sản phẩm '%s' (ID: %d) không đủ hàng. Còn lại: %d, Yêu cầu: %d",
                        product.getName(), product.getId(), product.getStockQuantity(), item.getQuantity());
                log.warn("[PRODUCT-SERVICE] [RESERVE FAILED] {}", errMsg);
                throw new RuntimeException(errMsg);
            }
        }

        // Pha 2: Trừ tồn kho nếu tất cả đều hợp lệ
        for (StockReservationItem item : items) {
            Product product = productRepository.findById(item.getProductId()).get();
            int newQuantity = product.getStockQuantity() - item.getQuantity();
            product.setStockQuantity(newQuantity);
            productRepository.save(product);
            log.info("[PRODUCT-SERVICE] [RESERVE SUCCESS] Đã trừ {} sản phẩm '{}' (ID: {}). Tồn kho mới: {}",
                    item.getQuantity(), product.getName(), product.getId(), newQuantity);
        }

        return StockOperationResponse.builder()
                .success(true)
                .message("Đã giữ kho thành công cho các sản phẩm trong đơn hàng.")
                .build();
    }

    @Override
    @Transactional
    public StockOperationResponse releaseStock(List<StockReservationItem> items) {
        log.info("[PRODUCT-SERVICE] [COMPENSATING ACTION: RELEASE STOCK] Bắt đầu hoàn trả lại kho cho {} mặt hàng", items.size());

        for (StockReservationItem item : items) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int restoredQuantity = product.getStockQuantity() + item.getQuantity();
                product.setStockQuantity(restoredQuantity);
                productRepository.save(product);
                log.info("[PRODUCT-SERVICE] [COMPENSATE SUCCESS] Đã hoàn lại {} sản phẩm '{}' (ID: {}). Tồn kho sau hoàn: {}",
                        item.getQuantity(), product.getName(), product.getId(), restoredQuantity);
            });
        }

        return StockOperationResponse.builder()
                .success(true)
                .message("Đã hoàn trả tồn kho thành công (Compensating action executed).")
                .build();
    }
}

