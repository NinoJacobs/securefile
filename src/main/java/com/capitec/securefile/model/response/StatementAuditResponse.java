package com.capitec.securefile.model.response;

import java.util.List;

public record StatementAuditResponse(
        String statementId,
        List<StatementAuditEventResponse> events) {}
