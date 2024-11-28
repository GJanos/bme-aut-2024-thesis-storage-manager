package com.bme.vik.aut.thesis.depot.general.alert;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InternalReorder {
    private final ProductSchema productSchema;
    private final String productDescription;
    private final LocalDateTime expiresAt;

    public InternalReorder(ProductSchema productSchema, String productDescription, LocalDateTime expiredAt) {
        this.productSchema = productSchema;
        this.productDescription = productDescription;
        this.expiresAt = expiredAt;
    }
}
