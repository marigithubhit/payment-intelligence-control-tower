package com.mari.iso20022.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String code, String message) {
}
