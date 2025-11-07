package com.training.customer_service.dtos;

import com.training.customer_service.enums.FiberStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiberDropLineResponse {
    private Long id;
    private Long customerId;
    private Long fromSplitterId;
    private Double lengthMeters;
    private FiberStatus status;
}
