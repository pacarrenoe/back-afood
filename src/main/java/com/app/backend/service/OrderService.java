package com.app.backend.service;

import com.app.backend.dto.OrderRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface OrderService {
    ResponseEntity<Map<String, Object>> createOrder(OrderRequest request);
    ResponseEntity<Map<String, Object>> listOrders();
    ResponseEntity<Map<String, Object>> updateStatus(Long id, String status);
}
