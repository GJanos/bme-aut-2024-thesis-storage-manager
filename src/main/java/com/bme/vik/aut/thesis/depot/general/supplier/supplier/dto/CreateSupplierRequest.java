package com.bme.vik.aut.thesis.depot.general.supplier.supplier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateSupplierRequest {
    private String name;
    private String email;
    private Integer lowStockAlertThreshold;
    private Integer expiryAlertThreshold;
    private Integer reorderThreshold;
    private Integer reorderQuantity;
}
