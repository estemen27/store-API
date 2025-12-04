package com.store.api.service;

import com.store.api.client.BoldClient;
import com.store.api.dto.CreateOrderItemRequestDTO;
import com.store.api.dto.CreateOrderRequestDTO;
import com.store.api.dto.CreateOrderResponseDTO;
import com.store.api.dto.OrderItemResponseDTO;
import com.store.api.dto.OrderResponseDTO;
import com.store.api.dto.ProductDTO;
import com.store.api.exception.ResourceNotFoundException;
import com.store.api.model.Order;
import com.store.api.model.OrderItem;
import com.store.api.model.OrderStatus;
import com.store.api.repository.OrderRepository;
import com.store.api.service.OrderService;
import com.store.api.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final BoldClient boldClient;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductService productService,
                            BoldClient boldClient) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.boldClient = boldClient;
    }

    @Override
    @Transactional
    public CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request) {

        // 1. Crear entidad Order con datos del cliente y envío
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddressLine1(request.getShippingAddressLine1());
        order.setShippingAddressLine2(request.getShippingAddressLine2());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDepartment(request.getShippingDepartment());
        order.setShippingCountry(request.getShippingCountry());
        order.setCurrency("COP");
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        // 2. Construir items y calcular total
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequestDTO itemReq : request.getItems()) {
            ProductDTO product = productService.getProductById(itemReq.getProductId());

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setSize(itemReq.getSize());
            orderItem.setColor(itemReq.getColor());
            orderItem.setImageUrl(product.getImageUrl());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setSubtotal(subtotal);

            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);

        // 3. Generar número de orden (antes del link de pago)
        order.setOrderNumber(generateOrderNumber());

        // 4. Generar link de pago en Bold usando la orden (ya con publicId y orderNumber)
        String paymentLink = boldClient.createPaymentLink(order);
        order.setPaymentLink(paymentLink);

        // 5. Guardar en BD
        Order saved = orderRepository.save(order);

        // 6. Construir respuesta
        CreateOrderResponseDTO response = new CreateOrderResponseDTO();
        response.setOrderId(saved.getPublicId());
        response.setOrderNumber(saved.getOrderNumber());
        response.setTotalAmount(saved.getTotalAmount());
        response.setCurrency(saved.getCurrency());
        response.setPaymentLink(saved.getPaymentLink());
        response.setStatus(saved.getStatus());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(UUID publicId) {
        Order order = orderRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getPublicId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setShippingAddressLine1(order.getShippingAddressLine1());
        dto.setShippingAddressLine2(order.getShippingAddressLine2());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingDepartment(order.getShippingDepartment());
        dto.setShippingCountry(order.getShippingCountry());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        dto.setPaymentLink(order.getPaymentLink());
        dto.setBoldPaymentId(order.getBoldPaymentId());
        dto.setAlegraInvoiceId(order.getAlegraInvoiceId());
        dto.setAlegraInvoiceNumber(order.getAlegraInvoiceNumber());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> {
                    OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
                    itemDto.setProductId(item.getProductId());
                    itemDto.setProductName(item.getProductName());
                    itemDto.setSize(item.getSize());
                    itemDto.setColor(item.getColor());
                    itemDto.setImageUrl(item.getImageUrl());
                    itemDto.setUnitPrice(item.getUnitPrice());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setSubtotal(item.getSubtotal());
                    return itemDto;
                })
                .collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }

    private static String generateOrderNumber() {
        // Implementación simple; luego puedes hacer algo más bonito/secuencial
        return "ORD-" + System.currentTimeMillis();
    }
}
