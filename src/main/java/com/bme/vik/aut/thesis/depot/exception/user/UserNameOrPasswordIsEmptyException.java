package com.bme.vik.aut.thesis.depot.exception.user;

public class UserNameOrPasswordIsEmptyException  extends RuntimeException {
    public UserNameOrPasswordIsEmptyException(String message) {
        super(message);
    }
}
