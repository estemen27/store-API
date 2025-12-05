package com.store.api.service;

import com.store.api.client.AlegraClient;
import com.store.api.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    private final AlegraClient alegraClient;

    public ProductServiceImpl(AlegraClient alegraClient) {
        this.alegraClient = alegraClient;
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        List<Map<String, Object>> items = alegraClient.getProducts();

        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(this::mapAlegraItemToProductDTO)
                .toList();
    }

    @Override
    public ProductDTO getProductById(String productId) {
        Map<String, Object> alegraItem = alegraClient.getProductById(productId);
        return mapAlegraItemToProductDTO(alegraItem);
    }

    @SuppressWarnings("unchecked")
    private ProductDTO mapAlegraItemToProductDTO(Map<String, Object> alegraItem) {
        ProductDTO dto = new ProductDTO();

        // Manejo seguro de ID y Nombre
        dto.setId(String.valueOf(alegraItem.get("id")));
        dto.setName((String) alegraItem.get("name"));
        dto.setDescription((String) alegraItem.getOrDefault("description", ""));

        // --- LÓGICA DE PRECIO CORREGIDA (Sin error de compilación) ---
        Object rawPrice = alegraItem.get("price");
        BigDecimal finalPrice = BigDecimal.ZERO;

        try {
            if (rawPrice instanceof List<?>) {
                List<?> priceList = (List<?>) rawPrice;
                if (!priceList.isEmpty()) {
                    Object firstPriceObj = priceList.get(0);

                    if (firstPriceObj instanceof Map) {
                        // AQUÍ ESTABA EL ERROR: Hacemos cast explícito a Map<String, Object>
                        Map<String, Object> priceMap = (Map<String, Object>) firstPriceObj;

                        // Buscamos "price" primero, si es nulo buscamos "value"
                        Object innerPrice = priceMap.get("price");
                        if (innerPrice == null) {
                            innerPrice = priceMap.get("value");
                        }

                        finalPrice = new BigDecimal(String.valueOf(innerPrice != null ? innerPrice : "0"));
                    } else {
                        // Es un número directo en la lista: [100]
                        finalPrice = new BigDecimal(String.valueOf(firstPriceObj));
                    }
                }
            } else if (rawPrice instanceof Number) {
                // Es un número directo: "price": 100
                finalPrice = new BigDecimal(String.valueOf(rawPrice));
            } else if (rawPrice instanceof String) {
                // Es un String: "price": "100"
                finalPrice = new BigDecimal((String) rawPrice);
            }
        } catch (Exception e) {
            System.err.println("Error parseando precio para ID " + dto.getId() + ": " + rawPrice);
            finalPrice = BigDecimal.ZERO;
        }

        dto.setPrice(finalPrice);
        // -------------------------------------------------------------

        dto.setCurrency("COP");
        dto.setImageUrl((String) alegraItem.getOrDefault("imageUrl", null));
        dto.setColor((String) alegraItem.getOrDefault("color", null));

        dto.setSizes(List.of());
        dto.setStock(null);

        return dto;
    }
}