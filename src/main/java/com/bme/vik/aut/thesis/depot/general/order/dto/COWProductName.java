package com.bme.vik.aut.thesis.depot.general.order.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class COWProductName implements CreateOrderRequest {
    private String productName;
    private int quantity;

    @Override
    public String getSupplierName() {
        return null;
    }
}
