package com.store.api.service;

import com.store.api.client.AlegraClient;
import com.store.api.dto.ProductDTO;
import com.store.api.service.ProductService;
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

        // TODO: ajustar mapping según la respuesta real de Alegra
        return items.stream()
                .map(this::mapAlegraItemToProductDTO)
                .toList();
    }

    @Override
    public ProductDTO getProductById(String productId) {
        Map<String, Object> alegraItem = alegraClient.getProductById(productId);
        return mapAlegraItemToProductDTO(alegraItem);
    }

    private ProductDTO mapAlegraItemToProductDTO(Map<String, Object> alegraItem) {
        ProductDTO dto = new ProductDTO();
        dto.setId(String.valueOf(alegraItem.get("id")));
        dto.setName((String) alegraItem.get("name"));
        dto.setDescription((String) alegraItem.getOrDefault("description", ""));
        dto.setPrice(new BigDecimal(String.valueOf(alegraItem.getOrDefault("price", "0"))));
        dto.setCurrency("COP"); // o leerlo de Alegra si viene
        dto.setImageUrl((String) alegraItem.getOrDefault("imageUrl", null));
        dto.setColor((String) alegraItem.getOrDefault("color", null));
        // sizes y stock dependen del modelo de Alegra
        dto.setSizes(List.of());
        dto.setStock(null);
        return dto;
    }
}
