package com.store.api.client;


import java.util.List;
import java.util.Map;

public interface AlegraClient {

    List<Map<String, Object>> getProducts();

    Map<String, Object> getProductById(String productId);

    // Luego puedes agregar crear factura, etc.
}
