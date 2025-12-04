package com.store.api.dto;

import com.store.api.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponseDTO {

    private UUID id;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingDepartment;
    private String shippingCountry;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentLink;
    private String boldPaymentId;
    private String alegraInvoiceId;
    private String alegraInvoiceNumber;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponseDTO> items;
}
