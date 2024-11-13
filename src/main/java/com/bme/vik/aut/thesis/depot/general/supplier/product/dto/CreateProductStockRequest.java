package com.bme.vik.aut.thesis.depot.general.supplier.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductStockRequest {
    private Long productSchemaId;
    private String description;
    private int quantity;
    private LocalDateTime expiresAt;
}
