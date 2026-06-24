package com.capitec.securefile.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
@Builder
public class StatementDetailResponse {
    String statementId;
    String statementName;
    String customerId;
    String accountNumberMasked;
    LocalDate periodStart;
    LocalDate periodEnd;
    OffsetDateTime generatedAt;
    String fileName;
    long fileSizeBytes;
    String contentType;
    String downloadUrl;
    OffsetDateTime downloadUrlExpiresAt;
}
