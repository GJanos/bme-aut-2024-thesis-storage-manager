package com.bme.vik.aut.thesis.depot.general.alert.event;

import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import com.bme.vik.aut.thesis.depot.general.supplier.product.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ProductExpiredAlertEvent extends ApplicationEvent {
    private final Inventory inventory;
    private final List<Product> products;

    public ProductExpiredAlertEvent(Object source, Inventory inventory, List<Product> products) {
        super(source);
        this.inventory = inventory;
        this.products = products;
    }
}
