# Securefile

## Architecture

See [Securefile Architecture](docs/architecture.md) for the system boundaries covering API, auth, database, object storage, metrics, CI, profiles, and local infrastructure.

## How To Run

Start PostgreSQL and LocalStack S3:

```bash
chmod +x docker/localstack/init/01-create-bucket.sh
docker compose -f docker-compose.yaml up --build
```

Run the application:

```bash
./gradlew bootRun
```

Run the build/test task:

```bash
./gradlew test
```

Reset local infrastructure and reseed PostgreSQL:

```bash
docker compose -f docker-compose.yaml down -v
docker compose -f docker-compose.yaml up --build
```

Local login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer.one","password":"password"}'
```

Refresh an expired access token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

Use the returned access token:

```bash
curl http://localhost:8080/api/v1/customers/me/statements \
  -H "Authorization: Bearer <accessToken>"
```

Seeded users:

```text
admin.user      / password
customer.one    / password
customer.two    / password
customer.three  / password
```

## Verify Postgres And S3

Check containers:

```bash
docker compose -f docker-compose.yaml ps
```

Verify PostgreSQL is accepting connections:

```bash
docker exec securefile-postgres pg_isready -U admin -d securefile
```

Verify seeded PostgreSQL data:

```bash
docker exec securefile-postgres psql -U admin -d securefile -c "
SELECT 'users' AS table_name, count(*) FROM users
UNION ALL SELECT 'customers', count(*) FROM customers
UNION ALL SELECT 'accounts', count(*) FROM accounts
UNION ALL SELECT 'account_transactions', count(*) FROM account_transactions
UNION ALL SELECT 'statements', count(*) FROM statements
ORDER BY table_name;"
```

Expected seed counts:

```text
users:                4
customers:            3
accounts:             3
account_transactions: 300
statements:           12
```

Verify LocalStack is running:

```bash
curl http://localhost:4566/_localstack/health
```

Verify the S3 bucket exists:

```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test aws --endpoint-url=http://localhost:4566 s3 ls
```

Expected bucket:

```text
securefile-statements
```

Verify application health:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## Local Monitoring

The Docker Compose stack also runs Prometheus and Grafana for local observability.

Start the full stack:

```bash
docker compose -f docker-compose.yaml up --build
```

Access Prometheus:

```text
http://localhost:8082
```

Prometheus scrapes the Spring Boot application from:

```text
http://securefile-app:8080/actuator/prometheus
```

From your host machine, you can check the raw application metrics at:

```bash
curl http://localhost:8080/actuator/prometheus
```

Access Grafana:

```text
http://localhost:8083
```

Default local Grafana login:

```text
username: admin
password: admin
```

Grafana is provisioned automatically with the Prometheus datasource and Securefile dashboards from:

```text
docker/grafana/provisioning
docker/grafana/dashboards
```

Provisioned dashboards:

```text
Securefile Overview              App uptime, startup time, request rate, JVM memory, CPU usage
Securefile Service Overview      Request rate, 5xx error rate, average response time, status codes
Securefile API Traffic           Requests by endpoint, 4xx responses, 5xx responses
Securefile JVM Health            Heap, non-heap, GC pauses, live threads
Securefile Database Pool         Hikari active/idle/pending connections and timeouts
Securefile Security and Auth     Login attempts, login failures, rate-limit blocks, token refreshes
Securefile Statement Operations  Statement generation, downloads, download failures, S3 failures
```

Custom Securefile metrics added:

```text
securefile_auth_login_attempts_total
securefile_auth_login_failures_total
securefile_auth_rate_limit_blocks_total
securefile_auth_token_refresh_total
securefile_statement_generation_total
securefile_statement_download_total
securefile_statement_download_failures_total
securefile_s3_upload_failures_total
securefile_s3_download_failures_total
```

If a dashboard shows no data, check that the app is being scraped:

```bash
curl "http://localhost:8082/api/v1/query?query=up%7Bjob%3D%22securefile%22%7D"
```

The expected result should include `"value":[...,"1"]`. Some panels show `0` until you generate traffic, for example by logging in, refreshing a token, generating a statement, or downloading a statement.

## CI And Release Workflow

The GitHub Actions release workflow is intentionally simulated. It models the expected build, quality, container promotion, deployment approval, and rollback stages, but it does not deploy to real INT, QA, or PROD infrastructure.

This is useful for demonstrating the release shape before real hosting exists. A real deployment would need actual environment targets, registry publishing, runtime secrets, smoke checks, and rollback implementation.

## Todo For SE2
- Add real integration tests with Testcontainers for PostgreSQL and LocalStack.

## Todo For SE3
- Add indexes, constraints, validation, transaction boundaries, pagination, and tests around edge cases like missing statements, expired tokens, and unauthorized access.

## out of scope
- Add login rate limiting.
- Add a scheduler to refresh standard 1 month, 3 month, 6 month, and 9 month statements.
- Add dependency vulnerability scanning in CI.
- Move secrets to AWS secret manager/github actions
- kubernetes for autoscaling, load balancing, etc. 
