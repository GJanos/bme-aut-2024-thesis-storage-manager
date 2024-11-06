package com.bme.vik.aut.thesis.depot.exception.supplier;

public class NonGreaterThanZeroProductStockAddException extends RuntimeException {
    public NonGreaterThanZeroProductStockAddException(String message) {
        super(message);
    }
}
