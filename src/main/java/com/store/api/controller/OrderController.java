package com.store.api.controller;


import com.store.api.dto.CreateOrderRequestDTO;
import com.store.api.dto.CreateOrderResponseDTO;
import com.store.api.dto.OrderResponseDTO;
import com.store.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponseDTO> createOrder(
            @Valid @RequestBody CreateOrderRequestDTO request) {

        CreateOrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
}
