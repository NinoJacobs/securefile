package com.capitec.securefile.model.response;

import com.capitec.securefile.database.enums.StatementStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
@Builder
public class StatementDetailResponse {
    String statementId;
    String customerId;
    String accountNumberMasked;
    LocalDate periodStart;
    LocalDate periodEnd;
    OffsetDateTime generatedAt;
    StatementStatus status;
    String fileName;
    long fileSizeBytes;
    String contentType;
}
