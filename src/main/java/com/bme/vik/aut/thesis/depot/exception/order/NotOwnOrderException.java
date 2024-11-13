package com.bme.vik.aut.thesis.depot.exception.order;

public class NotOwnOrderException extends RuntimeException {
    public NotOwnOrderException(String message) {
        super(message);
    }
}
