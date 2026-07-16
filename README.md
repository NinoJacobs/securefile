# Securefile

Securefile is a Spring Boot API for authentication and customer statement access. It runs locally with PostgreSQL, LocalStack S3, Prometheus, and Grafana through Docker Compose.

## Architecture

See [Securefile Architecture](docs/architecture.md) for the system boundaries covering API, auth, database, object storage, metrics, CI, profiles, and local infrastructure.

## First-Time Setup

Prerequisites:

```text
Java 25
Docker
Docker Compose
```

Install Java 25:

- Homebrew option on macOS:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install openjdk@25
sudo ln -sfn "$(brew --prefix)/opt/openjdk@25/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-25.jdk
```

- Oracle JDK 25 downloads for macOS, Windows, and Linux  
  https://www.oracle.com/java/technologies/downloads/
- Optional for macOS/Linux: install via SDKMAN  
  https://sdkman.io/install/

Verify Java:

```bash
java --version
javac --version
```

The version output should start with `25`.

Install Docker and Docker Compose:

- Homebrew option on macOS:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install --cask docker-desktop
```

Docker Desktop includes Docker Compose. Start Docker Desktop once after install, then verify:

```bash
docker --version
docker compose version
```

- macOS: install Docker Desktop for Mac  
  https://docs.docker.com/desktop/setup/install/mac-install/
- Windows: install Docker Desktop for Windows  
  https://docs.docker.com/desktop/setup/install/windows-install/
- Ubuntu/Linux: install Docker Engine, then install the Docker Compose plugin  
  https://docs.docker.com/engine/install/ubuntu/  
  https://docs.docker.com/compose/install/linux/

Verify the install:

```bash
docker --version
docker compose version
```

Start the full local stack:

```bash
./gradlew clean
./gradlew bootJar
docker compose -f docker-compose.yaml up --build
```

This starts:

```text
securefile-app          Spring Boot API on port 8080
securefile-postgres     PostgreSQL on host port 5433
securefile-localstack   Local S3-compatible storage on port 4566
securefile-prometheus   Prometheus on port 8082
securefile-grafana      Grafana on port 8083
```

Run tests:

```bash
./gradlew test
```

Reset local data and rebuild from scratch:

```bash
docker compose -f docker-compose.yaml down -v
docker compose -f docker-compose.yaml build --no-cache app
docker compose -f docker-compose.yaml up
```

## Useful URLs

```text
Application health:  http://localhost:8080/actuator/health
Swagger UI:          http://localhost:8080/swagger-ui.html
OpenAPI JSON:        http://localhost:8080/api-docs
Prometheus:          http://localhost:8082
Grafana:             http://localhost:8083
```

Grafana login:

```text
username: admin
password: admin
```

## Seeded Users

```text
admin.user      / password
customer.one    / password
customer.two    / password
customer.three  / password
```

## Auth Flow

The auth flow is:

```text
login -> refresh token
```

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer.one","password":"password"}'
```

Refresh an access token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

Call a protected customer endpoint:

```bash
curl http://localhost:8080/api/v1/customers/me/statements \
  -H "Authorization: Bearer <accessToken>"
```

## Database Migrations

The project uses Flyway for versioned database migrations. Migration files live in:

```text
src/main/resources/db/migration
```

The application runs migrations at startup and uses Hibernate `ddl-auto: validate`, so schema changes should be made through Flyway instead of generated automatically by Hibernate.

## Local Verification

Check containers:

```bash
docker compose -f docker-compose.yaml ps
```

Verify PostgreSQL:

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

Verify LocalStack:

```bash
curl http://localhost:4566/_localstack/health
```

Verify the S3 bucket:

```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test aws --endpoint-url=http://localhost:4566 s3 ls
```

Expected bucket:

```text
securefile-statements
```

## Monitoring

Prometheus scrapes Spring Boot Actuator metrics from:

```text
app:8080/actuator/prometheus
```

From your host machine, view the raw metrics at:

```bash
curl http://localhost:8080/actuator/prometheus
```

Prometheus is available at:

```text
http://localhost:8082
```

Check that Prometheus is scraping the app:

```bash
curl "http://localhost:8082/api/v1/query?query=up%7Bjob%3D%22securefile%22%7D"
```

The expected result includes `"value":[...,"1"]`.

Grafana is available at:

```text
http://localhost:8083
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

Important exposed metrics:

```text
up
http_server_requests_seconds_count
jvm_memory_used_bytes
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_pending
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

Some panels show `0` until you generate traffic by logging in, refreshing a token, generating a statement, or downloading a statement.

## CI/CD

The GitHub Actions workflow is intentionally simulated because there is no real server yet. It shows the expected release path without deploying to real INT, QA, or PROD infrastructure.

Pull request path:

```text
Build Application -> Quality Checks -> Build Container
```

Merged or manual release path:

```text
Build Release Artifact -> Build And Push Container -> Simulate Deploy INT -> Simulate Deploy QA -> Simulate Deploy PROD
```

Rollback path:

```text
Simulate Rollback INT / QA / PROD
```

## Roadmap

```text
Add login rate limiting.
Add a scheduler to refresh standard 1 month, 3 month, 6 month, and 9 month statements.
Move secrets to AWS Secrets Manager or GitHub Actions secrets.
Add Kubernetes for autoscaling, load balancing, and production orchestration.
```
