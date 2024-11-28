package com.bme.vik.aut.thesis.depot.general.alert.event;

import com.bme.vik.aut.thesis.depot.general.admin.productschema.ProductSchema;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class LowStockAlertEvent extends ApplicationEvent {
    private final Inventory inventory;
    private final Map<ProductSchema, List<Product>> stock;

    public LowStockAlertEvent(Object source, Inventory inventory, Map<ProductSchema, List<Product>> stock) {
        super(source);
        this.inventory = inventory;
        this.stock = stock;
    }
}