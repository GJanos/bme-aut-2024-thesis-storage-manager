package com.bme.vik.aut.thesis.depot.exception.order;

public class NonGreaterThanZeroCreateOrderRequestException extends RuntimeException {
    public NonGreaterThanZeroCreateOrderRequestException(String message) {
        super(message);
    }
}
