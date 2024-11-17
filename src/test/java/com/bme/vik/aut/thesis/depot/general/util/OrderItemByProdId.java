package com.bme.vik.aut.thesis.depot.general.util;

import java.util.Map;

public class OrderItemByProdId extends OrderItem {
    private final Long productId;

    public OrderItemByProdId(Long productId) {
        this.productId = productId;
    }

    @Override
    public Map<String, Object> toOrderItemRequest() {
        return Map.of(
                "type", "productId",
                "productId", productId,
                "quantity", 1
        );
    }

    @Override
    public Long getProductId() {
        return productId;
    }

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