# Securefile

Securefile stores customer account statements as PDF files and provides secure, time-limited download links to customers.

## Current Design

The system is built around two flows:

1. Admins create statements for customers.
2. Customers list and download their own statements.

A statement is stored as metadata in PostgreSQL and as a PDF file in local storage. The database stores the file reference, not the PDF binary.

Download links are generated on demand and are not stored in the database. Each link is signed, scoped to a statement and customer, and expires after a short time.

## Database Tables

Main tables:

```text
roles
users
customers
statements
statement_generation_requests
```

Important relationships:

```text
users.role_id -> roles.id
customers.user_id -> users.id
statements.customer_id -> customers.id
statement_generation_requests.customer_id -> customers.id
statement_generation_requests.statement_id -> statements.id
```

The current schema and seed data live in:

```text
src/main/resources/db/data/001_create_all_tables.sql
```

Seed data includes:

```text
2 roles
6 users
5 customers
15 statements
8 statement generation requests
```

Each seeded customer has 3 statements.

## Local Postgres

A Dockerfile is provided for local PostgreSQL.

Database settings:

```text
Database: securefile
Username: admin
Password: admin
Port: 5432
```

Build and run:

```bash
docker build -t securefile-postgres .
docker run --name securefile-postgres -p 5432:5432 securefile-postgres
```

If the container already exists:

```bash
docker rm -f securefile-postgres
docker build -t securefile-postgres .
docker run --name securefile-postgres -p 5432:5432 securefile-postgres
```

Postgres init scripts only run when the database is first initialized. If the SQL changes, recreate the container.

## Application Config

Current local config in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/securefile
    username: admin
    password: admin
  jpa:
    hibernate:
      ddl-auto: none

securefile:
  storage:
    statement-directory: storage/statements
  download-link:
    secret: local-dev-download-link-secret-change-me
```

Uploaded PDFs are stored locally under:

```text
storage/statements
```

## Customer Endpoints

Base path:

```text
/api/v1/customers/me/statements
```

Important current limitation:

```text
/customers/me currently uses the first customer in the database.
Real authentication has not been implemented yet.
```

### List My Statements

```http
GET /api/v1/customers/me/statements
```

Returns all statements for the current customer.

Response type:

```text
List<StatementSummaryResponse>
```

Fields:

```text
statementId
customerId
accountNumberMasked
periodStart
periodEnd
generatedAt
status
```

### Get Statement Detail

```http
GET /api/v1/customers/me/statements/{statementId}
```

Returns statement details and creates a fresh time-limited download link.

Response type:

```text
StatementDetailResponse
```

Fields:

```text
statementId
customerId
accountNumberMasked
periodStart
periodEnd
generatedAt
status
fileName
fileSizeBytes
contentType
downloadUrl
downloadUrlExpiresAt
```

Security rule:

```text
Customer statement lookup must always use statement_id + customer_id.
Never fetch by statement_id alone for customer endpoints.
```

### Create Download Link Explicitly

```http
POST /api/v1/customers/me/statements/{statementId}/download-link
```

Returns a fresh download link without returning the full statement detail.

This endpoint still exists for compatibility, but the preferred flow is:

```text
GET statement detail -> receive fresh downloadUrl
```

Response type:

```text
DownloadLinkResponse
```

Fields:

```text
statementId
url
expiresAt
```

### Download Statement PDF

```http
GET /api/v1/customers/me/statements/{statementId}/download?token={token}
```

Validates the signed token and returns the PDF file.

The token is:

```text
scoped to the statement ID
scoped to the customer ID
time-limited
not stored in the database
```

## Admin Endpoints

Base path:

```text
/api/v1/admin
```

### List Statements For Customer

```http
GET /api/v1/admin/customers/{customerId}/statements
```

`customerId` can currently be either:

```text
numeric customers.id
customer_number like CUST-0001
```

Returns:

```text
List<StatementSummaryResponse>
```

### Upload Statement PDF

```http
POST /api/v1/admin/customers/{customerId}/statements/upload
Content-Type: multipart/form-data
```

Form fields:

```text
file          PDF file
statementName statement display name
periodStart   ISO date, for example 2026-01-01
periodEnd     ISO date, for example 2026-01-31
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/admin/customers/CUST-0001/statements/upload \
  -F "file=@statement.pdf;type=application/pdf" \
  -F "statementName=April 2026 Statement" \
  -F "periodStart=2026-04-01" \
  -F "periodEnd=2026-04-30"
```

Behavior:

```text
validates that the upload is a PDF
stores the PDF under storage/statements/{customerNumber}/...
creates a row in statements
returns StatementDetailResponse
```

The upload response does not need a download link, because this is an admin operation.

### Request Statement Generation

```http
POST /api/v1/admin/customers/{customerId}/statements/generate
Content-Type: application/json
```

Request body:

```json
{
  "periodStart": "2026-04-01",
  "periodEnd": "2026-04-30",
  "statementType": "ACCOUNT_STATEMENT"
}
```

Behavior:

```text
creates a row in statement_generation_requests
sets status to PENDING
does not generate the PDF yet
```

Response type:

```text
GenerationRequestResponse
```

## Statement Generation Status

Actual PDF generation is not implemented yet.

Current behavior:

```text
Admin generate endpoint queues a request only.
A future worker/service should process PENDING requests.
When generation completes, it should store the PDF and create/update a statements row.
```

Expected future generation flow:

```text
1. Admin requests generation.
2. Backend inserts statement_generation_requests row with PENDING.
3. Worker picks up PENDING request.
4. Worker marks request PROCESSING.
5. Worker generates PDF.
6. Worker stores PDF in configured storage.
7. Worker creates statements row.
8. Worker links statement_generation_requests.statement_id to statements.id.
9. Worker marks request COMPLETED.
```

## Current Service Behavior

Main service:

```text
src/main/java/com/capitec/securefile/service/StatementApiService.java
```

Implemented methods:

```text
listMyStatements()
getMyStatement(String statementId)
createDownloadLink(String statementId)
downloadStatement(String statementId, String token)
listStatementsForCustomer(String customerId)
uploadStatement(...)
generateStatement(...)
createGenerationRequest(...)
getGenerationRequest(...)
retryGenerationRequest(...)
```

`createGenerationRequest`, `getGenerationRequest`, and `retryGenerationRequest` are implemented enough to use the generation request table, but the separate `StatementGenerationRequestController` is not the main focus right now.

## Important Limitations

Authentication is not implemented.

```text
/customers/me uses the first customer in the database for now.
Admin endpoints are not protected yet.
```

Storage is local filesystem only.

```text
Production should use object storage such as S3, MinIO, or Azure Blob Storage.
```

Download links are application-signed links.

```text
They are not persisted.
They cannot be individually revoked yet.
Changing the download-link secret invalidates existing links.
```

Seeded statement PDFs do not physically exist unless files are uploaded.

```text
Seed data creates statement DB rows with file_key values.
The matching PDF files may not exist in storage/statements.
Download will return 404 until real files exist.
```

## Useful Test Commands

Compile:

```bash
./gradlew compileJava
```

Run app:

```bash
./gradlew bootRun
```

Connect from IntelliJ Database tool:

```text
Host: localhost
Port: 5432
Database: securefile
User: admin
Password: admin
```

## Next Work

Recommended next steps:

```text
Add authentication and replace the temporary first-customer lookup.
Protect admin endpoints by role.
Replace local storage with object storage.
Implement real PDF generation worker.
Add integration tests for the database-backed endpoints.
Move local secrets out of application.yaml for non-local environments.
```
