package com.bme.vik.aut.thesis.depot.exception;

public class UserNotFoundByIDException extends RuntimeException  {
    public UserNotFoundByIDException(String message) {
        super(message);
    }
}
