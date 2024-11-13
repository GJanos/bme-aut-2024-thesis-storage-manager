package com.bme.vik.aut.thesis.depot.exception.inventory;

public class InventoryFullException extends RuntimeException {
    public InventoryFullException(String message) {
        super(message);
    }
}
