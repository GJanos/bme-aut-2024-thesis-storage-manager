package com.bme.vik.aut.thesis.depot.exception;

import com.bme.vik.aut.thesis.depot.exception.category.CategoryAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.category.CategoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.DepotFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryFullException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.inventory.InventoryOutOfStockException;
import com.bme.vik.aut.thesis.depot.exception.order.*;
import com.bme.vik.aut.thesis.depot.exception.product.InvalidProductExpiryException;
import com.bme.vik.aut.thesis.depot.exception.product.ProductNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.productschema.NonGreaterThanZeroStorageSpaceException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.productschema.ProductSchemaNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.supplier.InvalidCreateSupplierRequestException;
import com.bme.vik.aut.thesis.depot.exception.supplier.NonGreaterThanZeroQuantityException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.supplier.SupplierNotFoundException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNameAlreadyExistsException;
import com.bme.vik.aut.thesis.depot.exception.user.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.exception.user.UserSupplierNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Helper method to build the error response
    private ResponseEntity<Map<String, String>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return new ResponseEntity<>(errorResponse, status);
    }

    /*******************************/
    /***** CATEGORY EXCEPTIONS *****/
    /*******************************/
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleCategoryAlreadyExistsException(CategoryAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCategoryNotFoundException(CategoryNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /********************************/
    /***** INVENTORY EXCEPTIONS *****/
    /********************************/
    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleInventoryNotFoundException(InventoryNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InventoryFullException.class)
    public ResponseEntity<Map<String, String>> handleInventoryFullException(InventoryFullException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InventoryOutOfStockException.class)
    public ResponseEntity<Map<String, String>> handleInventoryOutOfStockException(InventoryOutOfStockException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DepotFullException.class)
    public ResponseEntity<Map<String, String>> handleDepotFullException(DepotFullException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /****************************/
    /***** ORDER EXCEPTIONS *****/
    /****************************/
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFoundException(OrderNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCreateOrderRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCreateOrderRequestException(InvalidCreateOrderRequestException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TooLargeOrderException.class)
    public ResponseEntity<Map<String, String>> handleTooLargeOrderException(TooLargeOrderException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NonGreaterThanZeroCreateOrderRequestException.class)
    public ResponseEntity<Map<String, String>> handleNonGreaterThanZeroCreateOrderRequestException(NonGreaterThanZeroCreateOrderRequestException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductAlreadyReservedException.class)
    public ResponseEntity<Map<String, String>> handleProductAlreadyReservedException(ProductAlreadyReservedException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotOwnOrderException.class)
    public ResponseEntity<Map<String, String>> handleNotOwnOrderException(NotOwnOrderException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NonCancellableOrderException.class)
    public ResponseEntity<Map<String, String>> handleNonCancellableOrderException(NonCancellableOrderException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /******************************/
    /***** PRODUCT EXCEPTIONS *****/
    /******************************/
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFoundException(ProductNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidProductExpiryException.class)
    public ResponseEntity<Map<String, String>> handleInvalidProductExpiryException(InvalidProductExpiryException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /*************************************/
    /***** PRODUCT SCHEMA EXCEPTIONS *****/
    /*************************************/
    @ExceptionHandler(ProductSchemaAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleProductSchemaAlreadyExistsException(ProductSchemaAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ProductSchemaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductSchemaNotFoundException(ProductSchemaNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NonGreaterThanZeroStorageSpaceException.class)
    public ResponseEntity<Map<String, String>> handleNonGreaterThanZeroStorageSpaceException(NonGreaterThanZeroStorageSpaceException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /*******************************/
    /***** SUPPLIER EXCEPTIONS *****/
    /*******************************/
    @ExceptionHandler(SupplierAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleSupplierAlreadyExistsException(SupplierAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSupplierNotFoundException(SupplierNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NonGreaterThanZeroQuantityException.class)
    public ResponseEntity<Map<String, String>> handleNonGreaterThanZeroProductStockAddException(NonGreaterThanZeroQuantityException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCreateSupplierRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCreateSupplierRequestException(InvalidCreateSupplierRequestException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /*******************************/
    /***** USER EXCEPTIONS *********/
    /*******************************/
    @ExceptionHandler(UserNotFoundByIDException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundByIDException(UserNotFoundByIDException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserNameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserNameAlreadyExistsException(UserNameAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNameNotFoundError(UsernameNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserSupplierNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserSupplierNotFoundException(UserSupplierNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /*************************************/
    /***** AUTHENTICATION EXCEPTIONS *****/
    /*************************************/
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }
}
