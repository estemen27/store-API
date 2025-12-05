package com.store.api.dto;

import jakarta.validation.Valid; // IMPORTANTE: Agregar este import
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String customerName;

    @Email(message = "Email inválido")
    @NotBlank(message = "El email es obligatorio")
    private String customerEmail;

    private String customerPhone;

    @NotBlank(message = "La dirección (Line1) es obligatoria")
    private String shippingAddressLine1;

    private String shippingAddressLine2;

    @NotBlank(message = "La ciudad es obligatoria")
    private String shippingCity;

    private String shippingDepartment; // Opcional

    @NotBlank(message = "El país es obligatorio")
    private String shippingCountry;

    @NotEmpty(message = "La orden debe tener items")
    @Valid // <--- IMPORTANTE: Esto valida los campos internos de cada item (productId, quantity)
    private List<CreateOrderItemRequestDTO> items;
}