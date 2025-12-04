package com.store.api.client;

import com.store.api.exception.ResourceNotFoundException;
import com.store.api.model.Order;
import com.store.api.model.OrderStatus;
import com.store.api.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode; // Importante
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BoldClientImp implements BoldClient {

    private static final Logger log = LoggerFactory.getLogger(BoldClientImp.class);

    private final WebClient webClient;
    private final OrderRepository orderRepository;

    // Suprimimos la advertencia ya que planeas usarlo a futuro
    @SuppressWarnings("unused")
    private final String webhookSecret;

    public BoldClientImp(WebClient.Builder webClientBuilder,
                         OrderRepository orderRepository,
                         @Value("${bold.base-url}") String baseUrl,
                         @Value("${bold.api-key}") String apiKey,
                         @Value("${bold.webhook-secret:}") String webhookSecret) {

        this.orderRepository = orderRepository;
        this.webhookSecret = webhookSecret;

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBearerAuth(apiKey))
                .build();
    }

    @Override
    public String createPaymentLink(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", toMinorUnits(order.getTotalAmount()));
        payload.put("currency", order.getCurrency());
        payload.put("description", "Compra tienda de ropa - " + order.getOrderNumber());
        payload.put("reference", order.getPublicId().toString());

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", order.getCustomerName());
        customer.put("email", order.getCustomerEmail());
        customer.put("phone", order.getCustomerPhone());
        payload.put("customer", customer);

        Map<String, Object> response = webClient.post()
                .uri("/online/link/v1")
                .bodyValue(payload)
                .retrieve()
                // Corrección de tipos con Mono.<Throwable>error
                .onStatus(HttpStatusCode::is4xxClientError, res ->
                        res.bodyToMono(String.class).flatMap(body ->
                                Mono.<Throwable>error(new IllegalStateException("Error 4xx from Bold: " + body))))
                .onStatus(HttpStatusCode::is5xxServerError, res ->
                        res.bodyToMono(String.class).flatMap(body ->
                                Mono.<Throwable>error(new IllegalStateException("Error 5xx from Bold: " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null) {
            throw new IllegalStateException("Empty response from Bold when creating payment link");
        }

        Object urlObj = response.get("url");
        if (urlObj == null) {
            log.warn("Bold response without url field: {}", response);
            throw new IllegalStateException("Bold did not return a payment URL");
        }
        return urlObj.toString();
    }

    @Override
    public void processWebhook(Map<String, Object> payload) {
        log.info("Received webhook from Bold: {}", payload);

        String reference = (String) payload.get("reference");
        if (reference == null) {
            log.warn("Webhook without reference, cannot match order");
            return;
        }

        UUID orderPublicId;
        try {
            orderPublicId = UUID.fromString(reference);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid reference UUID in webhook: {}", reference);
            return;
        }

        String status = (String) payload.get("status");
        String paymentId = (String) payload.get("paymentId");

        Optional<Order> optionalOrder = orderRepository.findByPublicId(orderPublicId);
        if (optionalOrder.isEmpty()) {
            throw new ResourceNotFoundException("Order not found for reference " + reference);
        }

        Order order = optionalOrder.get();

        if (status == null) {
            log.warn("Webhook without status for order {}", reference);
            return;
        }

        switch (status.toUpperCase()) {
            case "APPROVED", "PAID" -> order.setStatus(OrderStatus.PAID);
            case "DECLINED", "REJECTED" -> order.setStatus(OrderStatus.FAILED);
            case "EXPIRED" -> order.setStatus(OrderStatus.FAILED);
            default -> log.info("Unhandled Bold status '{}' for order {}", status, reference);
        }

        if (paymentId != null) {
            order.setBoldPaymentId(paymentId);
        }

        orderRepository.save(order);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.longValue();
    }
}