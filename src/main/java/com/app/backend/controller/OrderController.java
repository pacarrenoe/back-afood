package com.app.backend.controller;

import com.app.backend.dto.OrderRequest;
import com.app.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COCINA')")
    public ResponseEntity<Map<String, Object>> listOrders() {
        return orderService.listOrders();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COCINA','ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return orderService.updateStatus(id, status);
    }
}
