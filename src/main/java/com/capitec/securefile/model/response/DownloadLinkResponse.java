package com.capitec.securefile.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class DownloadLinkResponse {
    String statementId;
    String url;
    OffsetDateTime expiresAt;
}
