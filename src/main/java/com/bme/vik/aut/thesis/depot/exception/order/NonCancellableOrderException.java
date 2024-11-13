package com.bme.vik.aut.thesis.depot.exception.order;

public class NonCancellableOrderException extends RuntimeException {
    public NonCancellableOrderException(String message) {
        super(message);
    }
}
