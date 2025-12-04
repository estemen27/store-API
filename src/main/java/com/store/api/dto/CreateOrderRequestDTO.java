package com.store.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequestDTO {

    @NotBlank
    private String customerName;

    @Email
    @NotBlank
    private String customerEmail;

    private String customerPhone;

    @NotBlank
    private String shippingAddressLine1;

    private String shippingAddressLine2;

    @NotBlank
    private String shippingCity;

    private String shippingDepartment;

    @NotBlank
    private String shippingCountry;

    @NotEmpty
    private List<CreateOrderItemRequestDTO> items;
}
