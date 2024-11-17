package com.bme.vik.aut.thesis.depot.general.report.dto;

import com.bme.vik.aut.thesis.depot.general.supplier.product.ExpiryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductExpiry {
    private Long productID;
    private String productName;
    private LocalDateTime expiresAt;
    private ExpiryStatus expiryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
