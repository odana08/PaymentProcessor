# Fee Calculator

Spring Boot REST API for processing payments and calculating fees.

## Requirements

- JDK 26
- Maven 3.6.3 or newer
- Docker with the Compose plugin
- curl and Python 3 for `scripts/api-test.sh`

Java 26 is the current project target. It is not an LTS release, so the target should be reviewed before a production deployment.

## Stack

- Spring Boot 4.1.1
- Spring MVC and Jakarta Validation
- Spring Data JPA and Hibernate
- Flyway
- MySQL 8.4
- H2 for automated tests

## Fee rules

| Payment type | Fee |
| --- | --- |
| `DOMESTIC_FEE` | 150 cents |
| `INTERNATIONAL_FEE` | 2% of the amount |
| `CHEQUE_FEE` | 200 up to 10,000; 500 up to 50,000; 1,000 above 50,000 |

Amounts and fees are stored with two decimal places. International fees are rounded with `RoundingMode.HALF_UP`.

## Architecture

```text
PaymentController
        ↓
PaymentProcessor
   ├── FeeCalculator → FeeRule implementations
   ├── NotificationSender implementations
   └── PaymentRepository
               ↓
        Spring Data JPA
               ↓
           Hibernate
               ↓
             MySQL
```

`PaymentController` handles HTTP input and output. `PaymentProcessor` contains transaction boundaries and payment processing. `PaymentRepository` is the database boundary.

The email and SMS senders currently write to standard output. They are placeholders for real notification integrations.

## Run locally

Clone the repository and prepare the local environment:

```bash
git clone git@gitlab.progressoft.io:ps.omar.dana/paymentprocessor.git FeeCalculator
cd FeeCalculator
cp .env.example .env
```

### Run with Docker

Build the application image and start it with MySQL:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

When the application has started, verify the API:

```bash
curl http://localhost:8080/api/payments/options
```

The built image is named `fee-calculator:local`. Stop the containers without deleting the MySQL volume:

```bash
docker compose down
```

### Run Spring Boot directly

Make sure `JAVA_HOME` points to JDK 26:

```bash
export JAVA_HOME=/path/to/jdk-26
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -version
```

Start MySQL:

```bash
docker compose up -d mysql
docker compose ps
docker compose logs --tail=50 mysql
```

Load the application variables and start Spring Boot:

```bash
set -a
. ./.env
set +a

SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

Interactive Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

The generated OpenAPI JSON document is available at:

```text
http://localhost:8080/v3/api-docs
```

To build and run the JAR:

```bash
mvn clean package
SPRING_PROFILES_ACTIVE=dev java -jar target/fee-calculator-1.0-SNAPSHOT.jar
```

## Configuration

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/fee_calculator?serverTimezone=UTC` |
| `DB_USERNAME` | `fee_app` |
| `DB_PASSWORD` | `fee_app_password` |
| `MYSQL_ROOT_PASSWORD` | `local_root_password` |
| `MYSQL_DATABASE` | `fee_calculator` |
| `MYSQL_USER` | `fee_app` |
| `MYSQL_PASSWORD` | `fee_app_password` |
| `MYSQL_PORT` | `3306` |

The values in `.env.example` are for local development only. `.env` is ignored by Git.

The `dev` profile logs Hibernate SQL and JDBC bind values. Do not enable bind-value logging in production.

## API

Base path: `/api/payments`

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/payments` | Process a payment | `201 Created` |
| `GET` | `/api/payments` | Search and page payments | `200 OK` |
| `GET` | `/api/payments/options` | List supported types and channels | `200 OK` |
| `GET` | `/api/payments/{reference}` | Find a payment | `200 OK` |
| `PUT` | `/api/payments/{reference}` | Replace editable payment details | `200 OK` |
| `PATCH` | `/api/payments/{reference}` | Update selected payment details | `200 OK` |
| `DELETE` | `/api/payments/{reference}` | Delete a payment | `204 No Content` |

The collection endpoint accepts:

- `status`
- `currency`
- `paymentType`
- `page`
- `size`
- `sort`

The default page is `0`, the default size is `20`, and the maximum size is `100`. Supported sort fields are `reference`, `type.name`, `amountInCents`, `currency`, `status`, and `feeInCents`.

### Create a payment

```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "paymentType": "DOMESTIC_FEE",
    "amountInCents": 10000.00,
    "currency": "JOD",
    "notificationChannels": ["EMAIL", "SMS"]
  }'
```

Example response:

```json
{
  "reference": "8a2dd71f-ccaa-4c49-82ea-17222b6ecfee",
  "paymentType": "DOMESTIC_FEE",
  "amountInCents": 10000.00,
  "currency": "JOD",
  "notificationChannels": ["EMAIL", "SMS"],
  "feeInCents": 150.00,
  "totalInCents": 10150.00,
  "status": "PROCESSED"
}
```

### Read and filter payments

```bash
REFERENCE=8a2dd71f-ccaa-4c49-82ea-17222b6ecfee

curl -i "http://localhost:8080/api/payments/$REFERENCE"
curl -i 'http://localhost:8080/api/payments?page=0&size=10&sort=reference,desc'
curl -i 'http://localhost:8080/api/payments?status=PROCESSED&currency=JOD&paymentType=domestic_fee'
```

### Replace or patch a payment

```bash
curl -i -X PUT "http://localhost:8080/api/payments/$REFERENCE" \
  -H 'Content-Type: application/json' \
  -d '{
    "paymentType": "CHEQUE_FEE",
    "amountInCents": 25000.00,
    "currency": "USD",
    "notificationChannels": ["SMS"]
  }'

curl -i -X PATCH "http://localhost:8080/api/payments/$REFERENCE" \
  -H 'Content-Type: application/json' \
  -d '{"amountInCents": 60000.00}'
```

PUT and PATCH recalculate the fee and run the selected notification senders.

### Delete a payment

```bash
curl -i -X DELETE "http://localhost:8080/api/payments/$REFERENCE"
```

## Errors

API errors use a consistent JSON response:

```json
{
  "timestamp": "2026-08-28T17:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_REQUEST",
  "message": "Invalid sort property: notAField",
  "path": "/api/payments",
  "violations": []
}
```

| Status | Code | Meaning |
| --- | --- | --- |
| `400` | `VALIDATION_FAILED` | One or more request fields failed Jakarta Validation; `violations` identifies them |
| `400` | `MALFORMED_REQUEST` | JSON, path, or query input could not be parsed |
| `400` | `INVALID_REQUEST` | A domain rule was violated, such as an unsupported type or duplicate channel |
| `404` | `PAYMENT_NOT_FOUND` | No payment exists for the supplied reference |
| `409` | `PAYMENT_CONFLICT` | An optimistic-lock conflict occurred during a concurrent update |

Unexpected exceptions remain `500 Internal Server Error` and use Spring Boot's non-disclosing default response.

## Database

Flyway creates two application tables:

- `payments`, keyed by UUID `reference`
- `payment_notification_channels`, keyed by payment reference and channel order

The channel table has a foreign key to `payments`, an `ON DELETE CASCADE`, and a unique constraint on payment reference plus channel name. Payment status, currency, and type have indexes used by the repository filters.

Hibernate runs with `ddl-auto=validate`; schema changes belong in new Flyway migrations under `src/main/resources/db/migration`.

Inspect local rows with:

```bash
docker compose exec mysql sh -lc \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  -e "SELECT reference, payment_type, amount_in_cents, currency, status, fee_in_cents, version FROM payments;"'
```

## Docker data

MySQL stores its files in the `mysql_data` named volume.

```bash
docker compose down
docker compose up -d mysql
```

The commands above recreate the container without deleting its data. To remove the local database as well:

```bash
docker compose down -v
```

## Tests

Run the Maven suite:

```bash
mvn test
```

The test profile uses H2 in MySQL compatibility mode and applies the same Flyway migration. The suite includes unit, MockMvc, repository, service integration, and random-port HTTP tests.

With the application running, exercise the API using:

```bash
./scripts/api-test.sh
```

Set `API_BASE_URL` to test another host or port:

```bash
API_BASE_URL=http://localhost:9090 ./scripts/api-test.sh
```
