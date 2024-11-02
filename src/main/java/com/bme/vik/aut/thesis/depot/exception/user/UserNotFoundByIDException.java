package com.bme.vik.aut.thesis.depot.exception.user;

public class UserNotFoundByIDException extends RuntimeException  {
    public UserNotFoundByIDException(String message) {
        super(message);
    }
}
