package com.training.customer_service.dtos;

import com.training.customer_service.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    @NotNull
    private CustomerStatus newStatus;
}
