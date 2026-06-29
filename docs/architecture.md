# Securefile Architecture

Securefile is a Spring Boot service for authenticating users and serving customer statement data. 
The application is designed to run locally with Docker Compose and to expose enough operational signals for 
local development and CI validation.

## System Boundaries

### API

The API layer exposes REST endpoints for authentication, customer statement access, and administrative statement operations. Controllers should stay thin: request validation, authentication context, response mapping, and delegation to services belong here. Business rules should live in service classes.

Swagger/OpenAPI is available through the Springdoc UI when the application is running.

### Auth

Authentication uses username/password login and JWT access tokens. Refresh tokens are issued separately from access tokens and are validated through the auth service. Customer-scoped endpoints should derive the customer identity from the authenticated user context rather than accepting customer IDs from the caller.

Auth-related metrics are exported for login attempts, login failures, rate-limit blocks, and token refreshes.

### Database

PostgreSQL is the application database. It stores users, customers, accounts, transactions, statements, and related metadata. Schema changes are managed with Flyway migrations so database structure and reference data are versioned with the codebase.

The application should prefer explicit constraints, indexes, and transaction boundaries over relying only on application logic.

### Object Storage

Statement files are stored separately from database metadata. Local development uses LocalStack S3, while production would use an S3-compatible object store. The database stores file metadata such as key, content type, size, and checksum; the object store holds the generated statement content.

S3 upload and download failures are exposed as application metrics.

### Metrics

Spring Boot Actuator exposes Prometheus metrics at `/actuator/prometheus`. The metric set includes JVM, HTTP, database pool, auth, statement, and object-storage signals.

Prometheus scrapes the application inside Docker Compose, and Grafana provides pre-provisioned dashboards for service health, API traffic, JVM health, database pool usage, auth activity, and statement operations.

### CI

GitHub Actions models the expected build and release flow. The current release workflow is intentionally simulated: it demonstrates build, quality, image promotion, environment approval, deploy, and rollback stages without deploying to real infrastructure.

Real deployment would need registry publishing, environment-specific secrets, infrastructure targets, smoke checks, and rollback implementation.

### Profiles

Configuration is split by Spring profile. Local configuration is intended for Docker Compose and developer machines. Test configuration should isolate automated tests from local services. Production configuration should receive secrets and environment-specific values from the runtime platform, not from committed files.

Required secrets should fail fast at startup when missing.

### Local Infrastructure

Docker Compose provides the local runtime dependencies:

```text
app         Spring Boot application
postgres    PostgreSQL database
localstack  Local S3-compatible object storage
prometheus  Metrics collection
grafana     Metrics dashboards
```

This local stack is meant to make development, debugging, and observability repeatable without needing cloud infrastructure.

