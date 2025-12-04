package com.store.api.client;


import com.store.api.model.Order;

import java.util.Map;

public interface BoldClient {

    String createPaymentLink(Order order);

    void processWebhook(Map<String, Object> payload);
}
