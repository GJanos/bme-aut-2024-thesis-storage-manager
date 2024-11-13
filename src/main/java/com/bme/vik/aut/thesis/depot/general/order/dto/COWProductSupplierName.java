package com.bme.vik.aut.thesis.depot.general.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class COWProductSupplierName implements CreateOrderRequest {
    private String supplierName;
    private String productName;
    private int quantity;
}
