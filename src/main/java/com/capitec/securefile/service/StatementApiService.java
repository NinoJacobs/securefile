package com.capitec.securefile.service;

import com.capitec.securefile.model.request.CreateGenerationRequestRequest;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.GenerationRequestResponse;
import com.capitec.securefile.database.enums.GenerationRequestStatus;
import com.capitec.securefile.model.response.StatementAuditEventResponse;
import com.capitec.securefile.model.response.StatementAuditResponse;
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

    private static final String CURRENT_CUSTOMER_ID = "customer-001";

    private final Map<String, StoredStatement> statementsById = new ConcurrentHashMap<>();
    private final Map<String, StoredGenerationRequest> generationRequestsById = new ConcurrentHashMap<>();
    private final Map<String, List<StatementAuditEventResponse>> auditEventsByStatementId = new ConcurrentHashMap<>();

    public List<StatementSummaryResponse> listMyStatements() {
        return listStatementsForCustomer(CURRENT_CUSTOMER_ID);
    }

    public StatementDetailResponse getMyStatement(String statementId) {
        return toDetailResponse(getStatementForCustomer(CURRENT_CUSTOMER_ID, statementId));
    }

    public DownloadLinkResponse createDownloadLink(String statementId) {
        StoredStatement statement = getStatementForCustomer(CURRENT_CUSTOMER_ID, statementId);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15);
        appendAuditEvent(
                statement.statementId(),
                "DOWNLOAD_LINK_CREATED",
                "CUSTOMER",
                CURRENT_CUSTOMER_ID,
                "Issued a temporary download link.");
        return new DownloadLinkResponse(
                statement.statementId(),
                "https://downloads.securefile.local/statements/%s?token=%s".formatted(
                        statement.statementId(), UUID.randomUUID()),
                expiresAt);
    }

    public GenerationRequestResponse createGenerationRequest(CreateGenerationRequestRequest request) {
        String requestId = "req-" + UUID.randomUUID();
        String statementId = "stmt-" + UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        StoredStatement statement = new StoredStatement(
                statementId,
                request.customerId(),
                maskedAccountNumberFor(request.customerId()),
                request.periodStart(),
                request.periodEnd(),
                null,
                StatementStatus.PROCESSING,
                "%s-%s-%s.pdf".formatted(
                        request.customerId(), request.periodStart(), request.periodEnd()),
                0L,
                "application/pdf");
        statementsById.put(statementId, statement);

        StoredGenerationRequest generationRequest = new StoredGenerationRequest(
                requestId,
                statementId,
                request.customerId(),
                request.statementType(),
                request.periodStart(),
                request.periodEnd(),
                GenerationRequestStatus.QUEUED,
                now,
                now);
        generationRequestsById.put(requestId, generationRequest);

        appendAuditEvent(
                statementId,
                "GENERATION_REQUEST_CREATED",
                "SYSTEM",
                request.customerId(),
                "Queued statement generation request %s.".formatted(requestId));

        return toGenerationRequestResponse(generationRequest);
    }

    public GenerationRequestResponse getGenerationRequest(String requestId) {
        return toGenerationRequestResponse(getGenerationRequestById(requestId));
    }

    public GenerationRequestResponse retryGenerationRequest(String requestId) {
        StoredGenerationRequest existingRequest = getGenerationRequestById(requestId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        StoredGenerationRequest retriedRequest = existingRequest.withStatus(GenerationRequestStatus.QUEUED, now);
        generationRequestsById.put(requestId, retriedRequest);

        StoredStatement statement = getStatementById(existingRequest.statementId());
        statementsById.put(statement.statementId(), statement.withStatus(StatementStatus.PROCESSING));

        appendAuditEvent(
                existingRequest.statementId(),
                "GENERATION_REQUEST_RETRIED",
                "SYSTEM",
                existingRequest.customerId(),
                "Retried statement generation request %s.".formatted(requestId));

        return toGenerationRequestResponse(retriedRequest);
    }

    public List<StatementSummaryResponse> listStatementsForCustomer(String customerId) {
        return statementsById.values().stream()
                .filter(statement -> statement.customerId().equals(customerId))
                .sorted(Comparator.comparing(StoredStatement::periodEnd).reversed())
                .map(this::toSummaryResponse)
                .toList();
    }

    public StatementAuditResponse getStatementAudit(String statementId) {
        getStatementById(statementId);
        List<StatementAuditEventResponse> events = auditEventsByStatementId.getOrDefault(statementId, List.of());
        return new StatementAuditResponse(statementId, events);
    }

    private StoredStatement getStatementForCustomer(String customerId, String statementId) {
        StoredStatement statement = getStatementById(statementId);
        if (!statement.customerId().equals(customerId)) {
            throw new ResponseStatusException(NOT_FOUND, "Statement not found");
        }
        return statement;
    }

    private StoredStatement getStatementById(String statementId) {
        StoredStatement statement = statementsById.get(statementId);
        if (statement == null) {
            throw new ResponseStatusException(NOT_FOUND, "Statement not found");
        }
        return statement;
    }

    private StoredGenerationRequest getGenerationRequestById(String requestId) {
        StoredGenerationRequest request = generationRequestsById.get(requestId);
        if (request == null) {
            throw new ResponseStatusException(NOT_FOUND, "Generation request not found");
        }
        return request;
    }

    private void appendAuditEvent(
            String statementId,
            String action,
            String actorType,
            String actorId,
            String detail) {
        auditEventsByStatementId.compute(statementId, (ignored, existingEvents) -> {
            List<StatementAuditEventResponse> events =
                    existingEvents == null ? new ArrayList<>() : new ArrayList<>(existingEvents);
            events.add(new StatementAuditEventResponse(
                    "audit-" + UUID.randomUUID(),
                    action,
                    actorType,
                    actorId,
                    OffsetDateTime.now(ZoneOffset.UTC),
                    detail));
            return List.copyOf(events);
        });
    }

    private StatementSummaryResponse toSummaryResponse(StoredStatement statement) {
        return new StatementSummaryResponse(
                statement.statementId(),
                statement.customerId(),
                statement.accountNumberMasked(),
                statement.periodStart(),
                statement.periodEnd(),
                statement.generatedAt(),
                statement.status());
    }

    private StatementDetailResponse toDetailResponse(StoredStatement statement) {
        return new StatementDetailResponse(
                statement.statementId(),
                statement.customerId(),
                statement.accountNumberMasked(),
                statement.periodStart(),
                statement.periodEnd(),
                statement.generatedAt(),
                statement.status(),
                statement.fileName(),
                statement.fileSizeBytes(),
                statement.contentType());
    }

    private GenerationRequestResponse toGenerationRequestResponse(StoredGenerationRequest request) {
        return new GenerationRequestResponse(
                request.requestId(),
                request.statementId(),
                request.customerId(),
                request.statementType(),
                request.periodStart(),
                request.periodEnd(),
                request.status(),
                request.submittedAt(),
                request.lastUpdatedAt());
    }

    private String maskedAccountNumberFor(String customerId) {
        return "****%s".formatted(Math.abs(customerId.hashCode()) % 10_000);
    }

    private void seedData() {
        StoredStatement januaryStatement = new StoredStatement(
                "stmt-1001",
                CURRENT_CUSTOMER_ID,
                "****1234",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                OffsetDateTime.parse("2026-02-01T09:15:00Z"),
                StatementStatus.AVAILABLE,
                "statement-2026-01.pdf",
                243_901L,
                "application/pdf");
        StoredStatement februaryStatement = new StoredStatement(
                "stmt-1002",
                CURRENT_CUSTOMER_ID,
                "****1234",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                OffsetDateTime.parse("2026-03-01T09:15:00Z"),
                StatementStatus.AVAILABLE,
                "statement-2026-02.pdf",
                251_884L,
                "application/pdf");
        StoredStatement adminStatement = new StoredStatement(
                "stmt-2001",
                "customer-002",
                "****8821",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                OffsetDateTime.parse("2026-03-03T13:45:00Z"),
                StatementStatus.AVAILABLE,
                "statement-2026-02.pdf",
                197_321L,
                "application/pdf");

        statementsById.put(januaryStatement.statementId(), januaryStatement);
        statementsById.put(februaryStatement.statementId(), februaryStatement);
        statementsById.put(adminStatement.statementId(), adminStatement);

        StoredGenerationRequest completedRequest = new StoredGenerationRequest(
                "req-1001",
                februaryStatement.statementId(),
                CURRENT_CUSTOMER_ID,
                "ACCOUNT_STATEMENT",
                februaryStatement.periodStart(),
                februaryStatement.periodEnd(),
                GenerationRequestStatus.COMPLETED,
                OffsetDateTime.parse("2026-02-28T07:00:00Z"),
                OffsetDateTime.parse("2026-03-01T09:15:00Z"));
        generationRequestsById.put(completedRequest.requestId(), completedRequest);

        auditEventsByStatementId.put(
                januaryStatement.statementId(),
                List.of(
                        new StatementAuditEventResponse(
                                "audit-1001",
                                "STATEMENT_GENERATED",
                                "SYSTEM",
                                "statement-engine",
                                OffsetDateTime.parse("2026-02-01T09:15:00Z"),
                                "Generated monthly statement."),
                        new StatementAuditEventResponse(
                                "audit-1002",
                                "STATEMENT_VIEWED",
                                "CUSTOMER",
                                CURRENT_CUSTOMER_ID,
                                OffsetDateTime.parse("2026-02-02T10:00:00Z"),
                                "Customer viewed statement metadata.")));
        auditEventsByStatementId.put(
                februaryStatement.statementId(),
                List.of(
                        new StatementAuditEventResponse(
                                "audit-1003",
                                "STATEMENT_GENERATED",
                                "SYSTEM",
                                "statement-engine",
                                OffsetDateTime.parse("2026-03-01T09:15:00Z"),
                                "Generated monthly statement.")));
        auditEventsByStatementId.put(
                adminStatement.statementId(),
                List.of(
                        new StatementAuditEventResponse(
                                "audit-2001",
                                "STATEMENT_GENERATED",
                                "SYSTEM",
                                "statement-engine",
                                OffsetDateTime.parse("2026-03-03T13:45:00Z"),
                                "Generated monthly statement."),
                        new StatementAuditEventResponse(
                                "audit-2002",
                                "ADMIN_ACCESSED",
                                "ADMIN",
                                "ops-user-17",
                                OffsetDateTime.parse("2026-03-04T08:20:00Z"),
                                "Operations review completed.")));
    }

    private record StoredStatement(
            String statementId,
            String customerId,
            String accountNumberMasked,
            LocalDate periodStart,
            LocalDate periodEnd,
            OffsetDateTime generatedAt,
            StatementStatus status,
            String fileName,
            long fileSizeBytes,
            String contentType) {

        private StoredStatement withStatus(StatementStatus updatedStatus) {
            return new StoredStatement(
                    statementId,
                    customerId,
                    accountNumberMasked,
                    periodStart,
                    periodEnd,
                    generatedAt,
                    updatedStatus,
                    fileName,
                    fileSizeBytes,
                    contentType);
        }
    }

    private record StoredGenerationRequest(
            String requestId,
            String statementId,
            String customerId,
            String statementType,
            LocalDate periodStart,
            LocalDate periodEnd,
            GenerationRequestStatus status,
            OffsetDateTime submittedAt,
            OffsetDateTime lastUpdatedAt) {

        private StoredGenerationRequest withStatus(
                GenerationRequestStatus updatedStatus,
                OffsetDateTime updatedAt) {
            return new StoredGenerationRequest(
                    requestId,
                    statementId,
                    customerId,
                    statementType,
                    periodStart,
                    periodEnd,
                    updatedStatus,
                    submittedAt,
                    updatedAt);
        }
    }
}
