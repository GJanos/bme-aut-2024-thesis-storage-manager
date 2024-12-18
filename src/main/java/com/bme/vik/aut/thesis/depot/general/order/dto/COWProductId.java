package com.bme.vik.aut.thesis.depot.general.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class COWProductId implements CreateOrderRequest {
    private Long productId;

    @Override
    public String getProductName() {
        return null;
    }

    @Override
    public String getSupplierName() {
        return null;
    }

    @Override
    public int getQuantity() {
        return 1;
    }
}