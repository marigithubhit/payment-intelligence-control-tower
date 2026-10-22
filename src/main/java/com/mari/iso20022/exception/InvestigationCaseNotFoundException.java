package com.mari.iso20022.exception;

import java.util.UUID;

public class InvestigationCaseNotFoundException extends RuntimeException {
    public InvestigationCaseNotFoundException(UUID caseId) {
        super("Investigation case " + caseId + " was not found");
    }
}
