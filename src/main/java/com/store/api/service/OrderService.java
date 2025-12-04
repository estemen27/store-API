package com.store.api.service;

import com.store.api.dto.CreateOrderRequestDTO;
import com.store.api.dto.CreateOrderResponseDTO;
import com.store.api.dto.OrderResponseDTO;

import java.util.UUID;

public interface OrderService {

    CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request);

    OrderResponseDTO getOrder(UUID publicId);
}

