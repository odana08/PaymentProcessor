# Fee Calculator

Spring Boot REST API for processing payments and calculating fees.

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

]
