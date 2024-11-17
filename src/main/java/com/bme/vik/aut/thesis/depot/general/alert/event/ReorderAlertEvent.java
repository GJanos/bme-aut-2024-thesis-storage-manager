package com.bme.vik.aut.thesis.depot.general.alert.event;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import org.springframework.context.ApplicationEvent;

public class ReorderAlertEvent extends ApplicationEvent {
    private final Inventory inventory;
    private final Product product;

    public ReorderAlertEvent(Object source, Inventory inventory, Product product) {
        super(source);
        this.inventory = inventory;
        this.product = product;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Product getProduct() {
        return product;
    }
}