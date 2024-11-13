package com.bme.vik.aut.thesis.depot.exception.productschema;

public class NonGreaterThanZeroStorageSpaceException extends RuntimeException {
    public NonGreaterThanZeroStorageSpaceException(String message) {
        super(message);
    }
}
