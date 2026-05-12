package com.payment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all payment-related errors.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public PaymentException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public PaymentException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
