package com.store.api.service;

import com.store.api.client.BoldClient;
import com.store.api.dto.CreateOrderItemRequestDTO;
import com.store.api.dto.CreateOrderRequestDTO;
import com.store.api.dto.CreateOrderResponseDTO;
import com.store.api.dto.OrderResponseDTO;
import com.store.api.dto.OrderItemResponseDTO;
import com.store.api.dto.ProductDTO;
import com.store.api.exception.ResourceNotFoundException;
import com.store.api.model.Order;
import com.store.api.model.OrderItem;
import com.store.api.model.OrderStatus;
import com.store.api.repository.OrderRepository;
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
        // 1. Construir la orden y asignar TODOS los datos del cliente
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());

        // --- AQUÍ ESTABA EL ERROR: Asignación explícita de la dirección ---
        order.setShippingAddressLine1(request.getShippingAddressLine1());
        order.setShippingAddressLine2(request.getShippingAddressLine2());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDepartment(request.getShippingDepartment());
        order.setShippingCountry(request.getShippingCountry());
        // ------------------------------------------------------------------

        order.setCurrency("COP");
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        // 2. Calcular Totales y crear Items
        BigDecimal total = BigDecimal.ZERO;

        // Inicializamos la lista de items si es necesario (aunque JPA suele manejarlo, es buena práctica)
        if (order.getItems() == null) {
            order.setItems(new java.util.ArrayList<>());
        }

        for (CreateOrderItemRequestDTO itemReq : request.getItems()) {
            ProductDTO product = productService.getProductById(itemReq.getProductId());

            // Protección contra precios nulos
            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(price);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setSubtotal(subtotal);

            // Datos visuales del item
            orderItem.setSize(itemReq.getSize());
            orderItem.setColor(itemReq.getColor());
            orderItem.setImageUrl(product.getImageUrl());

            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        order.setOrderNumber(generateOrderNumber());

        // 3. GUARDAR (Genera el UUID y persiste en BD)
        Order savedOrder = orderRepository.save(order);

        // 4. GENERAR FIRMA BOLD (Integrity Signature)
        String signature = boldClient.calculateIntegritySignature(savedOrder);

        // 5. Construir Respuesta para el Frontend
        CreateOrderResponseDTO response = new CreateOrderResponseDTO();
        response.setOrderId(savedOrder.getPublicId());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setCurrency(savedOrder.getCurrency());
        response.setStatus(savedOrder.getStatus());

        // Datos específicos para el botón de Bold
        response.setBoldIntegritySignature(signature);
        response.setBoldIdentityKey(boldClient.getIdentityKey());
        // URL de retorno (puedes ajustarla a tu frontend real luego)
        response.setBoldRedirectionUrl("http://localhost:8080/api/orders/" + savedOrder.getPublicId());

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

        // Mapeo completo de dirección
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
        return "ORD-" + System.currentTimeMillis();
    }
}