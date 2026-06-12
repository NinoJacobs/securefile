package com.capitec.securefile.model.response;

import java.time.OffsetDateTime;

public record StatementAuditEventResponse(
        String eventId,
        String action,
        String actorType,
        String actorId,
        OffsetDateTime occurredAt,
        String detail) {}
