package com.bme.vik.aut.thesis.depot.general.util;

import java.util.Map;

public abstract class OrderItem {
    public abstract Map<String, Object> toOrderItemRequest();

    public abstract Long getProductId();
    public abstract String getProductName();
    public abstract String getSupplierName();
    public abstract int getQuantity();
}