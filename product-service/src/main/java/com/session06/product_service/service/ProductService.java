package com.session06.product_service.service;

import com.session06.product_service.dto.StockOperationResponse;
import com.session06.product_service.dto.StockReservationItem;
import com.session06.product_service.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> getAllProducts();
    List<Product> getProductsByCategoryId(Long categoryId);
    Optional<Product> getProductById(Long id);
    Product createProduct(Product product);
    Product updateProduct(Long id, Product productDetails);
    void deleteProduct(Long id);

    StockOperationResponse reserveStock(List<StockReservationItem> items);
    StockOperationResponse releaseStock(List<StockReservationItem> items);
}

