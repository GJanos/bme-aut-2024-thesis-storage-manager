package com.bme.vik.aut.thesis.depot.general.alert.event;

import com.bme.vik.aut.thesis.depot.general.alert.InternalReorder;
import com.bme.vik.aut.thesis.depot.general.supplier.inventory.Inventory;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ReorderAlertEvent extends ApplicationEvent {
    private final Inventory inventory;
    private final List<InternalReorder> reorders;

    public ReorderAlertEvent(Object source, Inventory inventory, List<InternalReorder> reorders) {
        super(source);
        this.inventory = inventory;
        this.reorders = reorders;
    }
}