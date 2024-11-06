package com.bme.vik.aut.thesis.depot.general.supplier.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockResponse {
    private Long productSchemaId;
    private int quantity;
    private String response;
}
