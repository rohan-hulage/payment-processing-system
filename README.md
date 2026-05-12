# Payment Processing System

A production-grade, cloud-native payment processing system built with Java 17, Spring Boot 3.2, Spring Cloud Gateway, Apache Kafka, MySQL, and JWT authentication.

## Architecture Overview

```
                        ┌─────────────────────────────────────────────────────┐
                        │                   API Gateway :8080                  │
                        │         (JWT validation, routing, CORS)              │
                        └──────┬──────────────┬──────────────┬────────────────┘
                               │              │              │
                    ┌──────────▼──┐  ┌────────▼──────┐  ┌───▼──────────────┐
                    │ auth-service│  │payment-service│  │transaction-service│
                    │   :8081     │  │    :8082      │  │      :8083        │
                    └──────┬──────┘  └───────┬───────┘  └────────┬──────────┘
                           │                 │                    │
                    ┌──────▼──────┐          │ Kafka Events       │ Kafka Consumer
                    │  MySQL      │          ▼                    ▼
                    │  auth_db    │  ┌───────────────┐   ┌────────────────────┐
                    └─────────────┘  │     Kafka     │   │notification-service│
                                     │  (4 topics)   │   │      :8084         │
                    ┌─────────────┐  └───────────────┘   └────────────────────┘
                    │  MySQL      │
                    │ payment_db  │
                    └─────────────┘
                    ┌─────────────┐
                    │  MySQL      │
                    │transaction_db│
                    └─────────────┘
```

## Microservices

| Service              | Port | Description                                          |
|----------------------|------|------------------------------------------------------|
| api-gateway          | 8080 | Spring Cloud Gateway — routing, JWT validation, CORS |
| auth-service         | 8081 | User registration, login, JWT issuance               |
| payment-service      | 8082 | Payment initiation, processing, idempotency          |
| transaction-service  | 8083 | Transaction history via Kafka consumer               |
| notification-service | 8084 | Async email notifications via Kafka consumer         |

## Kafka Topics

| Topic               | Producer        | Consumers                                    |
|---------------------|-----------------|----------------------------------------------|
| payment.initiated   | payment-service | transaction-service, notification-service    |
| payment.processed   | payment-service | transaction-service, notification-service    |
| payment.failed      | payment-service | transaction-service, notification-service    |
| payment.completed   | payment-service | transaction-service, notification-service    |

## Key Features

- **JWT Authentication** — HMAC-SHA256 signed access tokens (15 min) + refresh tokens (7 days)
- **Idempotency** — `X-Idempotency-Key` header prevents duplicate payment processing; cached responses stored for 24 hours
- **Optimistic Locking** — `@Version` on the `Payment` entity prevents lost-update anomalies
- **Kafka Exactly-Once** — Idempotent producer + manual consumer acknowledgment
- **Consumer Deduplication** — `event_id` unique constraint prevents duplicate transaction records
- **Database Indexes** — Composite indexes on `(user_id, status)`, `(user_id, created_at)` for high-volume queries
- **Global Exception Handler** — Structured error responses with HTTP status codes and error codes
- **Flyway Migrations** — Schema versioning for all three databases
- **Bean Validation** — Input validation on all request DTOs

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose

## Quick Start

### Option 1: Docker Compose (recommended)

```bash
# 1. Build all modules
mvn clean package -DskipTests

# 2. Start infrastructure + all services
docker-compose up -d

# 3. Check service health
curl http://localhost:8080/actuator/health
```

### Option 2: Local Development

```bash
# 1. Start infrastructure only
docker-compose up -d zookeeper kafka mysql-auth mysql-payment mysql-transaction

# 2. Build the project
mvn clean install -DskipTests

# 3. Start each service (in separate terminals)
cd auth-service        && mvn spring-boot:run
cd payment-service     && mvn spring-boot:run
cd transaction-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd api-gateway         && mvn spring-boot:run
```

## API Reference

All requests go through the API Gateway at `http://localhost:8080`.

### Authentication

#### Register
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass1!",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass1!"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "username": "john_doe",
    "roles": ["ROLE_USER"]
  }
}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh-token
Authorization: Bearer {refreshToken}
```

### Payments

All payment endpoints require `Authorization: Bearer {accessToken}`.

#### Initiate Payment
```http
POST /api/v1/payments
Authorization: Bearer {accessToken}
X-Idempotency-Key: unique-client-generated-key-123
Content-Type: application/json

{
  "amount": 99.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "payerId": "user-123",
  "payeeId": "merchant-456",
  "description": "Order #789"
}
```

Response (201 Created):
```json
{
  "success": true,
  "message": "Payment initiated",
  "data": {
    "paymentId": "550e8400-e29b-41d4-a716-446655440000",
    "idempotencyKey": "unique-client-generated-key-123",
    "amount": 99.99,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "status": "COMPLETED",
    "payerId": "user-123",
    "payeeId": "merchant-456",
    "cachedResponse": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

Duplicate request (same idempotency key) returns 200 with `"cachedResponse": true`.

#### Get Payment
```http
GET /api/v1/payments/{paymentId}
Authorization: Bearer {accessToken}
```

#### List Payments
```http
GET /api/v1/payments?status=COMPLETED&page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Bearer {accessToken}
```

#### Cancel Payment
```http
DELETE /api/v1/payments/{paymentId}
Authorization: Bearer {accessToken}
```

### Transactions

#### Get Transaction
```http
GET /api/v1/transactions/{transactionId}
Authorization: Bearer {accessToken}
```

#### List Transactions by User
```http
GET /api/v1/transactions?status=COMPLETED&page=0&size=20
Authorization: Bearer {accessToken}
```

#### List Transactions by Payment
```http
GET /api/v1/transactions/payment/{paymentId}
Authorization: Bearer {accessToken}
```

## Payment Methods

| Value          | Description       |
|----------------|-------------------|
| CREDIT_CARD    | Credit card       |
| DEBIT_CARD     | Debit card        |
| BANK_TRANSFER  | Bank wire/ACH     |
| DIGITAL_WALLET | PayPal, Apple Pay |

## Payment Statuses

| Status     | Description                              |
|------------|------------------------------------------|
| PENDING    | Received, awaiting processing            |
| PROCESSING | Being processed by payment processor     |
| COMPLETED  | Successfully completed                   |
| FAILED     | Processing failed                        |
| REFUNDED   | Refunded after completion                |

## Supported Currencies

USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, INR, SGD

## Idempotency

The payment API uses idempotency keys to safely retry requests without creating duplicate payments.

1. Generate a unique key per payment attempt (UUID recommended)
2. Pass it in the `X-Idempotency-Key` header
3. If the request succeeds, the response is cached for 24 hours
4. Retrying with the same key returns the cached response (HTTP 200) without re-processing
5. The response includes `"cachedResponse": true` to indicate a replay

```bash
# First request — creates payment (201)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00, "currency": "USD", ...}'

# Retry with same key — returns cached response (200)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Idempotency-Key: same-key-as-above" \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00, "currency": "USD", ...}'
```

## Environment Variables

| Variable                  | Default                        | Description                    |
|---------------------------|--------------------------------|--------------------------------|
| `JWT_SECRET`              | (base64 encoded default)       | HMAC-SHA256 signing key        |
| `DB_USERNAME`             | `root`                         | MySQL username                 |
| `DB_PASSWORD`             | `root`                         | MySQL password                 |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`               | Kafka broker address           |
| `AUTH_SERVICE_URL`        | `http://localhost:8081`        | Auth service URL (gateway)     |
| `PAYMENT_SERVICE_URL`     | `http://localhost:8082`        | Payment service URL (gateway)  |
| `TRANSACTION_SERVICE_URL` | `http://localhost:8083`        | Transaction service URL        |
| `NOTIFICATION_SERVICE_URL`| `http://localhost:8084`        | Notification service URL       |
| `NOTIFICATION_EMAIL_ENABLED` | `false`                     | Enable email notifications     |
| `MAIL_HOST`               | `smtp.gmail.com`               | SMTP host                      |
| `MAIL_USERNAME`           | (empty)                        | SMTP username                  |
| `MAIL_PASSWORD`           | (empty)                        | SMTP password                  |

## Security Notes

- The default `JWT_SECRET` is for development only. **Always override in production** with a cryptographically random 256-bit key.
- Passwords are hashed with BCrypt (cost factor 12).
- All services use stateless JWT authentication — no server-side sessions.
- The API Gateway validates JWTs before forwarding requests to downstream services.
- Downstream services also validate JWTs independently (defense in depth).

## Project Structure

```
payment-processing-system/
├── pom.xml                          # Root multi-module POM
├── docker-compose.yml
├── README.md
├── common/                          # Shared DTOs, enums, events, exceptions
│   └── src/main/java/com/payment/common/
│       ├── dto/                     # ApiResponse, PaymentRequest, PaymentResponse, TransactionDTO
│       ├── enums/                   # PaymentStatus, PaymentMethod
│       ├── events/                  # PaymentEvent (Kafka message)
│       └── exception/               # PaymentException, DuplicateTransactionException
├── api-gateway/                     # Spring Cloud Gateway :8080
├── auth-service/                    # JWT auth :8081
├── payment-service/                 # Core payments :8082
├── transaction-service/             # Transaction history :8083
└── notification-service/            # Async notifications :8084
```

## Running Tests

```bash
# All tests
mvn test

# Specific module
mvn test -pl payment-service

# Skip tests during build
mvn clean package -DskipTests
```
