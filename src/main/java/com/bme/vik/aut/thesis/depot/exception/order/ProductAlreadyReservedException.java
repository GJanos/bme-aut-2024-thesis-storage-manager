package com.bme.vik.aut.thesis.depot.exception.order;

public class ProductAlreadyReservedException extends RuntimeException {
  public ProductAlreadyReservedException(String message) {
    super(message);
  }
}
