package com.store.api.dto;

import com.store.api.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateOrderResponseDTO {

    private UUID orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentLink;
    private OrderStatus status;
}
