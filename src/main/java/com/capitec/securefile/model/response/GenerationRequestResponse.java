package com.capitec.securefile.model.response;

import com.capitec.securefile.database.enums.GenerationRequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record GenerationRequestResponse(
        String requestId,
        String statementId,
        String customerId,
        String statementType,
        LocalDate periodStart,
        LocalDate periodEnd,
        GenerationRequestStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime lastUpdatedAt) {}
