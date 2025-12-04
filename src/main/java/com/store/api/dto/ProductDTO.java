package com.store.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private String color;
    private List<String> sizes;
    private Integer stock;
}
