# Securefile

Securefile stores customer statements as metadata in PostgreSQL and exposes customer/admin APIs for listing, generating, and downloading statements.

## Current State

What is implemented now:

```text
- PostgreSQL-backed customer and statement data
- Admin API to list customers
- Admin API to list statements for a customer
- Admin API to generate a fake statement object for a customer and upload it to LocalStack S3
- Customer API to list statements
- Customer API to get a statement detail response that already includes a fresh download link
- Customer API to download a statement from LocalStack S3 using the signed token
- Local PostgreSQL container image
- LocalStack configured for S3 emulation
```

What is not implemented yet:

```text
- Real authentication and authorization
- Real PDF generation workflow
- Persisted download links
```

## Target Direction

The intended product direction is no longer admin-uploaded statement files as the primary workflow.

The target system should behave more like a real banking statement service:

```text
- Customer account and transaction data already exist in the system
- A customer requests a statement for a period such as 1 month, 3 months, 6 months, or a custom date range
- The backend generates a PDF statement from the underlying account transaction data
- The generated PDF is stored in S3-compatible object storage
- The database stores statement metadata plus the object key
- The customer receives a secure, time-limited download link for the generated PDF
```

What this means for the current codebase:

```text
- The current LocalStack S3 integration and object-key storage approach still fit the target design
- The current admin upload/generate flow is only a temporary placeholder
- The long-term source of truth for statements should be account and transaction data, not manually uploaded files
- Statement generation should become period-based and data-driven
```

Important storage note:

```text
The application stores statement objects in LocalStack S3.
The database stores the S3 object key in statements.file_key.
The bucket name is configuration-driven through securefile.s3.bucket.
```

## Project Structure

Main API contracts:

```text
src/main/java/com/capitec/securefile/api
```

Controllers:

```text
src/main/java/com/capitec/securefile/controller
```

Services:

```text
src/main/java/com/capitec/securefile/service
```

Database schema and seed scripts:

```text
src/main/resources/db/data/001_create_all_tables.sql
src/main/resources/db/data/002_seed_all_tables.sql
```

LocalStack init script:

```text
docker/localstack/init/01-create-bucket.sh
```

## Database Tables

Main tables:

```text
roles
users
customers
statements
statement_generation_requests
```

Relationships:

```text
users.role_id -> roles.id
customers.user_id -> users.id
statements.customer_id -> customers.id
```

Seed data:

```text
2 roles
6 users
5 customers
15 statements
```

## Local Infrastructure

Local container setup:

```text
docker-compose.yaml -> starts postgres + localstack together
Dockerfile          -> optional standalone local PostgreSQL image
```

Recommended local startup uses Compose.

### Start Postgres + LocalStack

Before first startup, make the LocalStack init script executable:

```bash
chmod +x docker/localstack/init/01-create-bucket.sh
```

If the script was edited with Windows line endings, normalize it:

```bash
sed -i '' $'s/\r$//' docker/localstack/init/01-create-bucket.sh
```

LocalStack executes files in `/etc/localstack/init/ready.d` directly, so the bucket init script must have the executable bit set.

```bash
docker compose -f docker-compose.yaml up --build
```

Services:

```text
PostgreSQL: localhost:5433
LocalStack: localhost:4566
Bucket:     securefile-statements
```

### Stop Everything

```bash
docker compose -f docker-compose.yaml down
```

### Reset Everything Including Data

```bash
docker compose -f docker-compose.yaml down -v
```

PostgreSQL init behavior:

```text
docker-compose.yaml mounts the schema and seed SQL files directly into
/docker-entrypoint-initdb.d inside the postgres container.
Those scripts run only when the postgres data volume is empty.
```

### Postgres Connection

```text
Host: localhost
Port: 5433
Database: securefile
Username: admin
Password: admin
```

### Verify Postgres

```bash
PGPASSWORD=admin psql -h localhost -p 5433 -U admin -d securefile
```

### Verify LocalStack S3

List buckets:

```bash
aws --endpoint-url=http://localhost:4566 s3 ls
```

Expected bucket:

```text
securefile-statements
```

If you prefer `awslocal`:

```bash
awslocal s3 ls
```

## Application Config

Current local config in [application.yaml](/Users/NinoJacobs/Capitec/dev/securefile/src/main/resources/application.yaml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/securefile
    username: admin
    password: admin
  jpa:
    hibernate:
      ddl-auto: none

securefile:
  s3:
    endpoint: http://localhost:4566
    region: us-east-1
    bucket: securefile-statements
    access-key: test
    secret-key: test
    path-style-access-enabled: true
  download-link:
    secret: local-dev-download-link-secret-change-me
```

Storage mapping:

```text
statements.file_key -> S3 object key inside securefile.s3.bucket
```

## Customer Endpoints

Base path:

```text
/api/v1/customers/me/statements
```

Current auth limitation:

```text
/customers/me currently resolves to the first customer in the database.
```

### List My Statements

```http
GET /api/v1/customers/me/statements
```

Returns:

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

Returns:

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

Important behavior:

```text
Fetching statement detail already gives you a fresh temporary download link.
There is no separate customer download-link creation endpoint anymore.
```

### Download Statement

```http
GET /api/v1/customers/me/statements/{statementId}/download?token={token}
```

Token rules:

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

### List Customers

```http
GET /api/v1/admin/customers
```

Returns:

```text
List<AdminCustomerResponse>
```

Fields:

```text
customerId
customerNumber
username
email
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

### Generate Statement For Customer

```http
POST /api/v1/admin/customers/{customerId}/statements/generate
```

Current behavior:

```text
Creates a fake statement row immediately.
Uses randomized generatedAt data within the last 30 days.
Returns StatementDetailResponse.
Does not queue a background generation job.
Does not require a request body.
```

## Service Split

Current service split:

```text
AdminStatementsService
CustomerStatementsService
StatementDomainSupportService
StatementDownloadLinkService
```

Purpose:

```text
AdminStatementsService            -> admin controller use cases
CustomerStatementsService         -> customer controller use cases
StatementDomainSupportService     -> shared customer/statement lookup logic
StatementDownloadLinkService      -> signed download-link creation and validation
```

## Build And Run

Compile:

```bash
./gradlew compileJava
```

Run the app:

```bash
./gradlew bootRun
```

## Next Work

Recommended next steps:

```text
Set up Liquibase for versioned schema migrations and reference-data bootstrap.
Add authentication and replace the temporary first-customer lookup.
Protect admin endpoints by role.
Introduce account and transaction domain tables as the source data for statement generation.
Replace the temporary admin upload/generate flow with period-based statement generation from account transactions.
Support customer statement requests for 1 month, 3 months, 6 months, and custom date ranges.
Replace the hand-built PDF generation with Apache PDFBox.
Add integration tests that match the current database-backed API.
Move local secrets out of application.yaml for non-local environments.
```
