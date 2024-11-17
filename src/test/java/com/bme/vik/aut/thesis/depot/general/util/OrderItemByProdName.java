package com.bme.vik.aut.thesis.depot.general.util;

import java.util.Map;

public class OrderItemByProdName extends OrderItem {
    private final String productName;
    private final int quantity;

    public OrderItemByProdName(String productName, int quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    @Override
    public Map<String, Object> toOrderItemRequest() {
        return Map.of(
                "type", "productName",
                "productName", productName,
                "quantity", quantity
        );
    }

    @Override
    public Long getProductId() {
        return null;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public String getSupplierName() {
        return null;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }
}