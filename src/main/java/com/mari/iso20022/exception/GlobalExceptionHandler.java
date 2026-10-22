package com.mari.iso20022.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidIso20022MessageException.class)
    public ResponseEntity<ApiError> handleInvalidMessage(InvalidIso20022MessageException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ISO20022_MESSAGE",
                "Unable to parse ISO 20022 payment message");
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiError> handleDuplicateTransaction(DuplicateTransactionException exception) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION", "Transaction has already been received");
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleTransactionNotFound(TransactionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvestigationCaseNotFoundException.class)
    public ResponseEntity<ApiError> handleInvestigationCaseNotFound(InvestigationCaseNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "INVESTIGATION_CASE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidReviewTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidReviewTransition(InvalidReviewTransitionException exception) {
        return error(HttpStatus.CONFLICT, "INVALID_REVIEW_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationFailure(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .findFirst().orElse("Review request is invalid");
        return error(HttpStatus.BAD_REQUEST, "INVALID_REVIEW_REQUEST", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String detail = cause.getMessage();
        String message = detail == null || detail.isBlank()
                ? "Request body is missing or cannot be deserialized"
                : "Request body cannot be deserialized: " + detail;
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_CONTENT", message);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType() {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_CONTENT_TYPE",
                "Content-Type must be application/xml");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message));
    }
}
