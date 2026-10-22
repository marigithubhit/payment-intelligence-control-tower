package com.mari.iso20022.exception;

public class InvalidIso20022MessageException extends RuntimeException {
    public InvalidIso20022MessageException(String message) {
        super(message);
    }

    public InvalidIso20022MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
