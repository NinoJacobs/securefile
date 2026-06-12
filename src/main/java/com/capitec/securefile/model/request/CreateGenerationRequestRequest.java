package com.capitec.securefile.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateGenerationRequestRequest(
        @NotBlank String customerId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotBlank String statementType) {}
