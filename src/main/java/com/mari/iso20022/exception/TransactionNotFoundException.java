package com.mari.iso20022.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String transactionId) {
        super("Transaction " + transactionId + " was not found");
    }
}
