# Codebase Reading Flow

This is the quickest way to understand the application if you are opening it for the first time.

The goal is not to read every file in order. The goal is to follow the main runtime flows and let the package structure make sense from there.

## 1. Start with the shape of the app

Read these first:

- `README.md`
- `build.gradle.kts`
- `src/main/java/com/capitec/securefile/SecurefileApplication.java`
- `src/main/resources/application.yaml`
- `src/main/resources/application-local.yaml`

This tells you:

- what the app does
- which frameworks are in play
- which external systems exist
- which config surfaces matter

Key things to notice:

- Spring Boot web app
- PostgreSQL via JPA
- Flyway migrations
- S3-backed statement storage
- JWT auth with refresh tokens
- Prometheus metrics exposed on `/actuator/prometheus`

## 2. Understand the package layout

Read the folders, not every file yet:

- `auth/` = login, JWT, security filter chain
- `controller/` = REST entry points
- `api/` = API interface and OpenAPI annotations
- `service/` = business logic
- `database/entity/` = JPA model
- `database/repository/` = data access
- `storage/` = S3 integration
- `common/` = cross-cutting config, mapping, exceptions, helpers
- `model/` = request/response DTOs

Once that picture is clear, follow the flows below.

## 3. Flow 1: Authentication

This is the first runtime flow to read because everything else depends on it.

Read in this order:

1. `src/main/java/com/capitec/securefile/auth/controller/AuthController.java`
2. `src/main/java/com/capitec/securefile/auth/service/AuthService.java`
3. `src/main/java/com/capitec/securefile/auth/service/SecurefileUserDetailsService.java`
4. `src/main/java/com/capitec/securefile/auth/service/JwtService.java`
5. `src/main/java/com/capitec/securefile/auth/security/SecurityConfig.java`
6. `src/main/java/com/capitec/securefile/auth/security/JwtAuthenticationFilter.java`
7. `src/main/java/com/capitec/securefile/auth/security/SecurefilePrincipal.java`
8. `src/main/java/com/capitec/securefile/common/util/CurrentUser.java`

What to understand here:

- how `/api/v1/auth/login` works
- how `/api/v1/auth/refresh` works
- how access tokens and refresh tokens differ
- how issuer/audience are applied
- how a request becomes an authenticated `SecurefilePrincipal`
- how services get the current customer ID

If this flow is clear, the rest of the app becomes easier.

## 4. Flow 2: Customer statement list and detail

This is the main read-only customer flow.

Read in this order:

1. `src/main/java/com/capitec/securefile/controller/CustomerStatementsController.java`
2. `src/main/java/com/capitec/securefile/api/CustomerStatementsApi.java`
3. `src/main/java/com/capitec/securefile/service/CustomerStatementsService.java`
4. `src/main/java/com/capitec/securefile/service/StatementDomainSupportService.java`
5. `src/main/java/com/capitec/securefile/database/repository/StatementRepository.java`
6. `src/main/java/com/capitec/securefile/common/mapper/StatementApiMapper.java`
7. `src/main/java/com/capitec/securefile/model/response/StatementSummaryResponse.java`
8. `src/main/java/com/capitec/securefile/model/response/StatementDetailResponse.java`

What to understand here:

- how `/api/v1/customers/me/statements` is assembled
- how custom statements are filtered from the list
- how download links are refreshed on read
- how the API response is mapped from the `Statement` entity

## 5. Flow 3: Statement generation

This is the main write/business workflow.

Read in this order:

1. `src/main/java/com/capitec/securefile/controller/CustomerStatementsController.java`
2. `src/main/java/com/capitec/securefile/model/request/StatementGenerationRequest.java`
3. `src/main/java/com/capitec/securefile/model/request/StatementPeriod.java`
4. `src/main/java/com/capitec/securefile/service/CustomerStatementsService.java`
5. `src/main/java/com/capitec/securefile/service/StatementGenerationService.java`
6. `src/main/java/com/capitec/securefile/service/StatementFileFactory.java`
7. `src/main/java/com/capitec/securefile/service/StatementDocumentService.java`
8. `src/main/java/com/capitec/securefile/database/repository/AccountRepository.java`
9. `src/main/java/com/capitec/securefile/database/repository/AccountTransactionRepository.java`
10. `src/main/java/com/capitec/securefile/database/repository/StatementRepository.java`

What to understand here:

- how the requested period becomes a date range
- how existing statements are reused
- how new statements are created safely
- how file names and object keys are generated
- how the PDF content is built

This is one of the most important flows in the codebase.

## 6. Flow 4: Downloading a statement

This ties security, storage, and generated content together.

Read in this order:

1. `src/main/java/com/capitec/securefile/controller/CustomerStatementsController.java`
2. `src/main/java/com/capitec/securefile/service/CustomerStatementsService.java`
3. `src/main/java/com/capitec/securefile/service/StatementDownloadLinkService.java`
4. `src/main/java/com/capitec/securefile/storage/service/StatementObjectStorageService.java`
5. `src/main/java/com/capitec/securefile/service/StatementGenerationService.java`

What to understand here:

- how download tokens are created and validated
- how the app checks whether the statement already exists in object storage
- how it falls back to regenerating content if needed
- how the final file response is returned to the client

## 7. Flow 5: Admin customer lookup

This is smaller, but it shows the admin side of the app.

Read in this order:

1. `src/main/java/com/capitec/securefile/controller/AdminStatementsController.java`
2. `src/main/java/com/capitec/securefile/api/AdminStatementsApi.java`
3. `src/main/java/com/capitec/securefile/service/AdminStatementsService.java`
4. `src/main/java/com/capitec/securefile/database/repository/CustomerRepository.java`
5. `src/main/java/com/capitec/securefile/model/response/AdminCustomerResponse.java`

What to understand here:

- how admin-only endpoints differ from customer endpoints
- how authorization boundaries are enforced

## 8. Flow 6: Data model and persistence

Once the runtime flows above make sense, read the data model.

Read in this order:

1. `src/main/java/com/capitec/securefile/database/entity/User.java`
2. `src/main/java/com/capitec/securefile/database/entity/Role.java`
3. `src/main/java/com/capitec/securefile/database/entity/Customer.java`
4. `src/main/java/com/capitec/securefile/database/entity/Account.java`
5. `src/main/java/com/capitec/securefile/database/entity/AccountTransaction.java`
6. `src/main/java/com/capitec/securefile/database/entity/Statement.java`
7. `src/main/resources/db/migration/V1__create_all_tables.sql`
8. `src/main/resources/db/migration/V2__seed_roles_and_admin_user.sql`
9. `src/main/resources/db/migration/V3__seed_customer_1.sql`
10. `src/main/resources/db/migration/V4__seed_customer_2.sql`
11. `src/main/resources/db/migration/V5__seed_customer_3.sql`
12. `src/main/resources/db/migration/V6__add_indexes_and_constraints.sql`

What to understand here:

- actual ownership relationships
- what the seeded data looks like
- where uniqueness and indexes are enforced
- which constraints the services rely on

## 9. Flow 7: Cross-cutting behavior

Read these after the core flows:

- `src/main/java/com/capitec/securefile/common/exception/HttpExceptionHandler.java`
- `src/main/java/com/capitec/securefile/common/exception/ErrorResponse.java`
- `src/main/java/com/capitec/securefile/common/config/OpenApiConfig.java`
- `src/main/java/com/capitec/securefile/common/config/RequiredConfigurationValidator.java`
- `src/main/java/com/capitec/securefile/storage/config/S3StorageConfig.java`
- `src/main/java/com/capitec/securefile/storage/config/SecurefileS3Properties.java`

These explain:

- how errors are shaped
- how config is validated on startup
- how storage wiring works
- how the public API is documented

## 10. Suggested reading sequence if you only have 30 minutes

If time is tight, do this:

1. `README.md`
2. `SecurityConfig.java`
3. `AuthController.java`
4. `AuthService.java`
5. `JwtService.java`
6. `CustomerStatementsController.java`
7. `CustomerStatementsService.java`
8. `StatementGenerationService.java`
9. `StatementDownloadLinkService.java`
10. `StatementObjectStorageService.java`
11. `Statement.java`
12. `V1__create_all_tables.sql`

That gets you the core architecture quickly.

## 11. Suggested reading sequence if you want to debug a bug

Use the request path to choose the flow:

- login issue -> start in `auth/`
- 401/403 issue -> `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `CurrentUser`
- statement list issue -> `CustomerStatementsController`, `CustomerStatementsService`, `StatementRepository`
- generation issue -> `StatementGenerationService`, repositories, `StatementFileFactory`, `StatementDocumentService`
- download issue -> `StatementDownloadLinkService`, `StatementObjectStorageService`, `CustomerStatementsService`
- schema/data issue -> `database/entity/`, `repository/`, `db/migration/`

## 12. Mental model of the app

At a high level:

1. user logs in and gets JWTs
2. JWT filter authenticates later requests
3. customer endpoints resolve the current customer from the JWT
4. services query PostgreSQL for accounts, transactions, and statement metadata
5. statement documents are generated as PDFs
6. PDFs are stored in S3-compatible object storage
7. download links are short-lived and validated separately

If you keep that model in your head while reading, the file structure becomes much easier to navigate.
