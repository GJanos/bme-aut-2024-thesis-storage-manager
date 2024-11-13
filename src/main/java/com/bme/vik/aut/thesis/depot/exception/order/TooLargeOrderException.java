package com.bme.vik.aut.thesis.depot.exception.order;

public class TooLargeOrderException extends RuntimeException {
    public TooLargeOrderException(String message) {
        super(message);
    }
}
