package com.bme.vik.aut.thesis.depot.exception.supplier;

public class NonGreaterThanZeroQuantityException extends RuntimeException {
    public NonGreaterThanZeroQuantityException(String message) {
        super(message);
    }
}
