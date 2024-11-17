package com.bme.vik.aut.thesis.depot.general.util;

import java.util.Map;

public class OrderItemByProdSupplName extends OrderItem {
    private final String productName;
    private final String supplierName;
    private final int quantity;

    public OrderItemByProdSupplName(String productName, String supplierName, int quantity) {
        this.productName = productName;
        this.supplierName = supplierName;
        this.quantity = quantity;
    }

    @Override
    public Map<String, Object> toOrderItemRequest() {
        return Map.of(
                "type", "productSupplierName",
                "supplierName", supplierName,
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
        return supplierName;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }
}