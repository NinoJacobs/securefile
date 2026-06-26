# Securefile

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

## Todo For SE2

- Add Liquibase or Flyway for versioned schema migrations and reference data.
- Add real integration tests with Testcontainers for PostgreSQL and LocalStack.
- Add GitHub Actions workflows for pull request build, test, and quality checks.
- Split configuration by profile: local, test
- Harden JWT handling with issuer, audience, stronger secret management, and short token TTLs.
- Add structured logging and request correlation IDs.
- Add metrics for statement generation, downloads, S3 failures, and database failures.

## out of scope
- Add login rate limiting.
- Add a scheduler to refresh standard 1 month, 3 month, 6 month, and 9 month statements.
- Add dependency vulnerability scanning in CI.
