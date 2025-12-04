package com.store.api.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderItemRequestDTO {

    @NotBlank
    private String productId;

    @Min(1)
    private Integer quantity;

    private String size;
    private String color;
}
