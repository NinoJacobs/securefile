package com.capitec.securefile.model.response;

import com.capitec.securefile.database.enums.StatementStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StatementSummaryResponse(
        String statementId,
        String customerId,
        String accountNumberMasked,
        LocalDate periodStart,
        LocalDate periodEnd,
        OffsetDateTime generatedAt,
        StatementStatus status) {}
