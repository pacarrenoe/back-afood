package com.app.backend.service;

import com.app.backend.model.Product;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ProductService {

    ResponseEntity<Map<String, Object>> listProducts(int page, int size);

    ResponseEntity<Map<String, Object>> getProduct(Long id);

    ResponseEntity<Map<String, Object>> createProduct(Product product);

    ResponseEntity<Map<String, Object>> updateProduct(Long id, Product updates);

    ResponseEntity<Map<String, Object>> deleteProduct(Long id);
}
