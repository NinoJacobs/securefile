package com.capitec.securefile.service;

import com.capitec.securefile.model.request.CreateGenerationRequestRequest;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.GenerationRequestResponse;
import com.capitec.securefile.database.enums.GenerationRequestStatus;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.database.enums.StatementStatus;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementApiService {

}
