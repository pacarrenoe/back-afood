package com.app.backend.service.impl;

import com.app.backend.dto.OrderRequest;
import com.app.backend.model.*;
import com.app.backend.repository.*;
import com.app.backend.service.AuditService;
import com.app.backend.service.OrderService;
import com.app.backend.utils.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditService auditService;

    @Override
    public ResponseEntity<Map<String, Object>> createOrder(OrderRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty())
            return ResponseUtil.error("Usuario no encontrado", Map.of(), 404);

        User user = userOpt.get();
        Order order = new Order();
        order.setUser(user);
        order.setStatus("pending");

        List<OrderItem> items = new ArrayList<>();
        double total = 0;

        for (OrderRequest.Item itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemReq.getProductId()));

            if (product.getStock() < itemReq.getQuantity())
                return ResponseUtil.error("Stock insuficiente para " + product.getName(), Map.of(), 400);

            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setSubtotal(product.getPrice() * itemReq.getQuantity());
            items.add(item);
            total += item.getSubtotal();
        }

        order.setItems(items);
        orderRepository.save(order);
        orderItemRepository.saveAll(items);

        auditService.logAction(user.getUsername(), "CREATE_ORDER", "Total: " + total);

        return ResponseUtil.success("Pedido creado correctamente", Map.of(
                "orderId", order.getId(),
                "total", total
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> listOrders() {
        List<Order> orders = orderRepository.findAll();
        return ResponseUtil.success("Listado de pedidos obtenido", Map.of("orders", orders));
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateStatus(Long id, String status) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty())
            return ResponseUtil.error("Pedido no encontrado", Map.of(), 404);

        Order order = orderOpt.get();
        order.setStatus(status);
        orderRepository.save(order);

        auditService.logAction("SYSTEM", "UPDATE_ORDER_STATUS", "Pedido #" + id + " → " + status);

        return ResponseUtil.success("Estado actualizado correctamente", Map.of("orderId", id, "status", status));
    }
}
