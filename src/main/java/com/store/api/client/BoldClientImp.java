package com.store.api.client;

import com.store.api.exception.ResourceNotFoundException;
import com.store.api.model.Order;
import com.store.api.model.OrderStatus;
import com.store.api.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BoldClientImp implements BoldClient {

    private static final Logger log = LoggerFactory.getLogger(BoldClientImp.class);

    private final OrderRepository orderRepository;
    private final String integritySecret;
    private final String identityKey; // La que va en el HTML data-api-key

    public BoldClientImp(OrderRepository orderRepository,
                         @Value("${bold.integrity-secret:}") String integritySecret,
                         @Value("${bold.identity-key:}") String identityKey) {
        this.orderRepository = orderRepository;
        this.integritySecret = integritySecret;
        this.identityKey = identityKey;
    }

    @Override
    public String getIdentityKey() {
        return this.identityKey;
    }

    // --- LÓGICA EXACTA PROPORCIONADA POR BOLD ---
    @Override
    public String calculateIntegritySignature(Order order) {
        // 1. Obtener datos exactos
        String identificador = order.getPublicId().toString();
        // Usamos toPlainString() para que BigDecimal no salga como 1.5E4
        String monto = order.getTotalAmount().toPlainString();
        String divisa = "COP"; // O order.getCurrency() si lo tienes dinámico
        String llaveSecreta = this.integritySecret;

        // 2. Concatenar: {Identificador}{Monto}{Divisa}{LlaveSecreta}
        String cadenaConcatenada = identificador + monto + divisa + llaveSecreta;

        log.info("Generando Hash para cadena: {}{}{}{HIDDEN}", identificador, monto, divisa);

        try {
            // 3. Convertir a bytes UTF-8
            byte[] encodedText = cadenaConcatenada.getBytes(StandardCharsets.UTF_8);

            // 4. Crear MessageDigest SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 5. Generar el hash
            byte[] hashBuffer = digest.digest(encodedText);

            // 6. Convertir a Hexadecimal (Código exacto de Bold)
            StringBuilder hashHex = new StringBuilder();
            for (byte b : hashBuffer) {
                hashHex.append(String.format("%02x", b));
            }

            return hashHex.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("Error generando hash SHA-256", e);
            throw new RuntimeException("Error en algoritmo de encriptación Bold", e);
        }
    }

    @Override
    public void processWebhook(Map<String, Object> payload) {
        // Lógica de webhook que ya tenías funcionando
        log.info("Webhook payload: {}", payload);
        String reference = (String) payload.get("reference");

        if (reference == null) return;

        // Intentar obtener status desde diferentes campos posibles según versión API
        String status = (String) payload.get("paymentStatus");
        if (status == null) status = (String) payload.get("status");

        UUID orderId;
        try {
            orderId = UUID.fromString(reference);
        } catch (Exception e) { return; }

        Optional<Order> orderOpt = orderRepository.findByPublicId(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (status != null) {
                switch (status.toUpperCase()) {
                    case "APPROVED", "PAID" -> order.setStatus(OrderStatus.PAID);
                    case "REJECTED", "FAILED", "DECLINED" -> order.setStatus(OrderStatus.FAILED);
                }
            }
            String txId = (String) payload.get("paymentId");
            if (txId != null) order.setBoldPaymentId(txId);
            orderRepository.save(order);
        }
    }
}