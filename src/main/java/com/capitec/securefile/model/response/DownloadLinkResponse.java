package com.capitec.securefile.model.response;

import java.time.OffsetDateTime;

public record DownloadLinkResponse(
        String statementId,
        String url,
        OffsetDateTime expiresAt) {}
