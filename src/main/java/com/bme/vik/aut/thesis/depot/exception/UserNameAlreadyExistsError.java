package com.bme.vik.aut.thesis.depot.exception;

public class UserNameAlreadyExistsError extends RuntimeException {
    public UserNameAlreadyExistsError(String message) {
        super(message);
    }
}
