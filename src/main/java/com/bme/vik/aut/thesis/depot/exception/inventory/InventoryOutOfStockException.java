package com.bme.vik.aut.thesis.depot.exception.inventory;

public class InventoryOutOfStockException extends RuntimeException {
    public InventoryOutOfStockException(String message) {
        super(message);
    }
}
