package com.app.backend.service.impl;

import com.app.backend.model.Product;
import com.app.backend.repository.ProductRepository;
import com.app.backend.service.AuditService;
import com.app.backend.service.ProductService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public ResponseEntity<Map<String, Object>> listProducts(int page, int size) {
        Page<Product> products = productRepository.findAll(PageRequest.of(page, size));
        return ResponseUtil.success("Productos obtenidos correctamente", Map.of(
                "content", products.getContent(),
                "totalElements", products.getTotalElements(),
                "totalPages", products.getTotalPages()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getProduct(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty())
            return ResponseUtil.error("Producto no encontrado", Map.of(), 404);

        return ResponseUtil.success("Producto encontrado correctamente", Map.of("product", productOpt.get()));
    }

    @Override
    public ResponseEntity<Map<String, Object>> createProduct(Product product) {
        if (productRepository.existsByName(product.getName()))
            return ResponseUtil.error("Ya existe un producto con ese nombre", Map.of(), 400);

        productRepository.save(product);
        auditService.logAction("SYSTEM", "CREATE_PRODUCT", "Producto creado: " + product.getName());
        return ResponseUtil.success("Producto creado correctamente", Map.of("id", product.getId()));
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateProduct(Long id, Product updates) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty())
            return ResponseUtil.error("Producto no encontrado", Map.of(), 404);

        Product product = productOpt.get();

        if (updates.getName() != null) product.setName(updates.getName());
        if (updates.getPrice() != null) product.setPrice(updates.getPrice());
        if (updates.getStock() != null) product.setStock(updates.getStock());
        if (updates.getCategory() != null) product.setCategory(updates.getCategory());
        if (updates.getImageUrl() != null) product.setImageUrl(updates.getImageUrl());

        productRepository.save(product);
        auditService.logAction("SYSTEM", "UPDATE_PRODUCT", "Producto actualizado: " + product.getName());
        return ResponseUtil.success("Producto actualizado correctamente", Map.of("id", product.getId()));
    }

    @Override
    public ResponseEntity<Map<String, Object>> deleteProduct(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty())
            return ResponseUtil.error("Producto no encontrado", Map.of(), 404);

        productRepository.delete(productOpt.get());
        auditService.logAction("SYSTEM", "DELETE_PRODUCT", "Producto eliminado: " + productOpt.get().getName());
        return ResponseUtil.success("Producto eliminado correctamente", null);
    }
}
