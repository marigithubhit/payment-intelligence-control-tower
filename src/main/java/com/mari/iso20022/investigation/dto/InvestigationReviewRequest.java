package com.mari.iso20022.investigation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvestigationReviewRequest(
        @NotNull ReviewDecision decision,
        @NotBlank @Size(min = 5, max = 2000) String reason,
        @NotBlank @Size(max = 100) String reviewer) {
}
