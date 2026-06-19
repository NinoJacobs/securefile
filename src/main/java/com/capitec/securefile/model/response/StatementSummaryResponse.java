package com.capitec.securefile.model.response;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
@Builder
public class StatementSummaryResponse {
    String statementId;
    String customerId;
    String accountNumberMasked;
    LocalDate periodStart;
    LocalDate periodEnd;
    OffsetDateTime generatedAt;
    String downloadUrl;
    OffsetDateTime downloadUrlExpiresAt;
}
