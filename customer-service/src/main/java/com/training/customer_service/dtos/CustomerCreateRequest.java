package com.training.customer_service.dtos;

import com.training.customer_service.enums.ConnectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String address;
    @NotBlank
    private String plan;
    @NotBlank
    private String neighborhood;
    @NotNull
    private ConnectionType connectionType;
}
