package com.bme.vik.aut.thesis.depot.general.order.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = COWProductId.class, name = "productId"),
        @JsonSubTypes.Type(value = COWProductName.class, name = "productName"),
        @JsonSubTypes.Type(value = COWProductSupplierName.class, name = "productSupplierName")
})
public interface CreateOrderRequest {
    String getProductName();
    String getSupplierName();
    int getQuantity();
}