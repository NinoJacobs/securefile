# Securefile

Securefile stores customer statements as metadata in PostgreSQL and exposes customer/admin APIs for listing, generating, and downloading statements.

## Current State

What is implemented now:

```text
- PostgreSQL-backed customer and statement data
- Admin API to list customers
- Admin API to generate period-based statements from customer account transactions
- Customer API to list statements
- Customer API to request statements for 1 month, 3 months, 6 months, 9 months, or a custom date range
- Customer API to get a statement detail response that already includes a fresh download link
- Customer API to download a statement from LocalStack S3 using the signed token
- JWT login backed by users.password_hash
- Admin/customer endpoint protection by role
- PDF generation with Apache PDFBox
- Local PostgreSQL container image
- LocalStack configured for S3 emulation
```

What is not implemented yet:

```text
- Persisted download links
```

## Target Direction

Statements are generated from customer account and transaction data.

The target system should behave more like a real banking statement service:

```text
- Customer account and transaction data already exist in the system
- A customer requests a statement for a period such as 1 month, 3 months, 6 months, 9 months, or a custom date range
- The backend generates a PDF statement from the underlying account transaction data
- The generated PDF is stored in S3-compatible object storage
- The database stores statement metadata plus the object key
- The customer receives a secure, time-limited download link for the generated PDF
```

What this means for the current codebase:

```text
- The current LocalStack S3 integration and object-key storage approach fits the design
- Account and transaction data are the source of truth for generated statements
- Statement generation is period-based and data-driven
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
src/main/resources/db/data/003_seed_all_tables_for_customer_1.sql
src/main/resources/db/data/004_seed_all_tables_for_customer_2.sql
src/main/resources/db/data/005_seed_all_tables_for_customer_3.sql
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
accounts
account_transactions
statements
```

Relationships:

```text
users.role_id -> roles.id
customers.user_id -> users.id
accounts.customer_id -> customers.id
account_transactions.account_id -> accounts.id
statements.customer_id -> customers.id
statements.account_id -> accounts.id
```

Seed data:

```text
2 roles
4 users
3 customers
3 accounts
300 transactions
12 statements
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
PostgreSQL: localhost:5432
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
Port: 5432
Database: securefile
Username: admin
Password: admin
```

### Verify Postgres

```bash
PGPASSWORD=admin psql -h localhost -p 5432 -U admin -d securefile
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
    url: jdbc:postgresql://localhost:5432/securefile
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

## Authentication

Login:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "username": "admin.user",
  "password": "password"
}
```

Response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "expiresAt": "2026-06-23T12:00:00Z"
}
```

Use the token on protected requests:

```http
Authorization: Bearer <jwt>
```

Customer JWT claims include customer identity:

```json
{
  "sub": "customer.one",
  "roles": ["ROLE_CUSTOMER"],
  "customerId": 1,
  "customerNumber": "CUST-0001",
  "exp": 1782216000
}
```

Admin JWTs do not include `customerId` or `customerNumber`.

Local users:

```text
admin.user      -> ADMIN
customer.one    -> CUSTOMER
customer.two    -> CUSTOMER
customer.three  -> CUSTOMER
```

All seeded local users use password:

```text
password
```

Role access:

```text
CUSTOMER users can access /api/v1/customers/me/**
ADMIN users can access /api/v1/admin/**
The /customers/me customer is resolved from the JWT customerId claim.
```

## Customer Endpoints

Base path:

```text
/api/v1/customers/me/statements
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
statementName
customerId
accountNumberMasked
periodStart
periodEnd
generatedAt
downloadUrl
downloadUrlExpiresAt
```

### Request My Statement

```http
POST /api/v1/customers/me/statements/generate?period=ONE_MONTH
POST /api/v1/customers/me/statements/generate?period=THREE_MONTHS
POST /api/v1/customers/me/statements/generate?period=SIX_MONTHS
POST /api/v1/customers/me/statements/generate?period=NINE_MONTHS
POST /api/v1/customers/me/statements/generate?period=CUSTOM&startMonth=2026-01&endMonth=2026-03
```

Supported `period` values:

```text
ONE_MONTH
THREE_MONTHS
SIX_MONTHS
NINE_MONTHS
CUSTOM
```

Behavior:

```text
Generates the statement PDF from account_transactions for the selected period.
Custom statements use only year and month input. The generated period starts on the first day of startMonth and ends on the last day of endMonth.
Does not create a statement when there are no account_transactions in the selected period.
Stores the PDF in S3-compatible object storage.
Stores statement metadata in statements.
Returns StatementDetailResponse with a fresh temporary download link.
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
statementName
customerId
accountNumberMasked
periodStart
periodEnd
generatedAt
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

### Generate Statement For Customer

```http
POST /api/v1/admin/customers/{customerId}/statements/generate
POST /api/v1/admin/customers/{customerId}/statements/generate?period=CUSTOM&startDate=2026-01-01&endDate=2026-03-31
```

`customerId` can currently be either:

```text
numeric customers.id
customer_number like CUST-0001
```

Behavior:

```text
Generates a PDF from the customer's active account transactions.
Supports ONE_MONTH, THREE_MONTHS, SIX_MONTHS, NINE_MONTHS, and CUSTOM date ranges.
Stores the generated PDF in object storage.
Returns StatementDetailResponse.
Does not queue a background generation job.
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
Add a scheduler to refresh standard 1 month, 3 month, 6 month, and 9 month statement records.
Add integration tests that match the current database-backed API.
Move local secrets out of application.yaml for non-local environments.
```
