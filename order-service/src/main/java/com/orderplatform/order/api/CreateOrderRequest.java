package com.orderplatform.order.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(@NotEmpty @Valid List<Line> lines) {

    public record Line(@NotBlank String sku, @Min(1) int quantity) {
    }
}
