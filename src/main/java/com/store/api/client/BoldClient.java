package com.store.api.client;

import com.store.api.model.Order;
import java.util.Map;

public interface BoldClient {
    // Genera el hash SHA-256 exacto como lo pide la documentación
    String calculateIntegritySignature(Order order);

    // Procesa el webhook de respuesta
    void processWebhook(Map<String, Object> payload);

    // Devuelve la llave de identidad para el frontend
    String getIdentityKey();
}