package com.mari.iso20022.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String transactionId) {
        super("Transaction already exists: " + transactionId);
    }

    public DuplicateTransactionException(String transactionId, Throwable cause) {
        super("Transaction already exists: " + transactionId, cause);
    }
}
