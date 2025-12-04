package com.store.api.controller;


import com.store.api.client.BoldClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/bold")
public class PaymentWebhookController {

    private final BoldClient boldClient;

    public PaymentWebhookController(BoldClient boldClient) {
        this.boldClient = boldClient;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        // TODO: validar firma con secret de Bold
        boldClient.processWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
