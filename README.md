# MandateGuard

MandateGuard is a global payment-authorization firewall for AI agents.

It allows individuals and businesses to define payment mandates that control how an AI agent may spend money. Every payment request is cryptographically signed, checked against the mandate, protected against replay attacks and recorded in an immutable audit trail.

## Problem

AI agents are increasingly able to book travel, purchase products, pay suppliers and manage subscriptions. Giving an autonomous agent unrestricted payment access creates serious risks:

- Unauthorized spending
- Duplicate payments
- Compromised agents
- Spending beyond approved limits
- Payments to disallowed merchants or countries
- Lack of human control for high-risk transactions
- Poor investigation and audit visibility

MandateGuard provides a security layer between an AI agent and a payment provider.

## Core Features

- Principal registration and JWT authentication
- RSA public-key registration for AI agents
- Signed AI-agent payment requests
- Currency, country, merchant and category restrictions
- Single-payment and total-budget limits
- Transaction-count limits
- Human approval for high-risk payments
- Mandate activation, suspension, resumption and revocation
- Nonce-based replay protection
- Idempotency protection
- Simulated payment-provider processing
- Budget release after provider failure
- PostgreSQL row locking against concurrent overspending
- Explainable authorization decisions
- Immutable security audit trail

## Payment Decisions

| Decision | Meaning |
|---|---|
| `ALLOWED` | The request satisfies all mandate rules |
| `DENIED` | The request violates a mandatory rule |
| `REQUIRES_APPROVAL` | The request satisfies the hard rules but requires human approval |

## Example

A principal creates this mandate for a travel AI agent:

```text
Currency: USD
Maximum single payment: $1,000
Total budget: $5,000
Human approval threshold: $500
Maximum transactions: 10
Countries: Nigeria, United Kingdom, United States
Merchants: Approved airlines and hotels
```

Results:

```text
$250 approved airline payment → ALLOWED
$750 approved airline payment → REQUIRES_APPROVAL
$1,200 payment → DENIED
Payment in EUR → DENIED
Payment to an unknown merchant → DENIED
```

## Request Flow

```text
AI Agent
   |
   | Signed payment request
   v
Signature Verification
   |
   v
Replay and Idempotency Checks
   |
   v
Mandate Rule Evaluation
   |
   +--> DENIED
   |
   +--> REQUIRES_APPROVAL --> Principal Approval
   |
   +--> ALLOWED
             |
             v
     Simulated Payment Provider
             |
             +--> EXECUTED
             |
             +--> FAILED --> Reserved budget released
```

## Technology Stack

- Java 17
- Spring Boot
- Spring Security
- OAuth 2.0 Resource Server
- JWT
- Spring Data JPA
- PostgreSQL
- Docker Compose
- Jakarta Validation
- RSA SHA-256 signatures
- JUnit 5
- AssertJ
- Maven

## Main Domain Objects

- `Principal` — person or business that owns funds
- `AiAgent` — autonomous agent authorized to request payments
- `PaymentMandate` — spending permission and restrictions
- `AgentPaymentRequest` — signed request submitted by an AI agent
- `PaymentApproval` — human decision for high-risk payments
- `AuditEvent` — immutable security and lifecycle record

## API Endpoints

### Authentication

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### AI Agents

```http
POST /api/v1/agents
GET  /api/v1/agents
POST /api/v1/agents/{agentId}/activate
```

### Payment Mandates

```http
POST /api/v1/mandates
GET  /api/v1/mandates
POST /api/v1/mandates/{mandateId}/activate
POST /api/v1/mandates/{mandateId}/suspend
POST /api/v1/mandates/{mandateId}/resume
POST /api/v1/mandates/{mandateId}/revoke
```

### Payment Requests

```http
POST /api/v1/payment-requests
GET  /api/v1/payment-requests
POST /api/v1/payment-requests/{paymentRequestId}/execute
```

A signed request uses these headers:

```http
X-Agent-Id: <agent UUID>
Idempotency-Key: <unique idempotency key>
X-Nonce: <unique request nonce>
X-Signature: <Base64 RSA signature>
```

### Human Approvals

```http
GET  /api/v1/approvals/pending
POST /api/v1/approvals/{approvalId}/approve
POST /api/v1/approvals/{approvalId}/reject
```

### Audit Events

```http
GET /api/v1/audit-events
GET /api/v1/audit-events/correlation/{correlationId}
```

## Running Locally

### Requirements

- Java 17 or later
- Docker Desktop
- Git
- Maven, or the included Maven wrapper

### Start PostgreSQL

```bash
docker compose up -d
```

Verify the containers:

```bash
docker compose ps
```

### Run the application

Git Bash or Linux:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts at:

```text
http://localhost:8080
```

Adminer is available at:

```text
http://localhost:8085
```

## Environment Variables

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
PORT
```

Use a strong secret containing at least 32 bytes for `JWT_SECRET`.

Never commit production credentials, JWT secrets or private keys.

## RSA Agent Keys

Generate a private key:

```bash
openssl genpkey \
  -algorithm RSA \
  -out local-keys/agent-private-key.pem \
  -pkeyopt rsa_keygen_bits:2048
```

Generate its public key:

```bash
openssl pkey \
  -in local-keys/agent-private-key.pem \
  -pubout \
  -out local-keys/agent-public-key.pem
```

Register only the public key with MandateGuard.

The private key must remain with the AI agent and must never be committed to Git.

## Automated Tests

Run all tests:

Git Bash or Linux:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

The test suite covers:

- Allowed payments
- Approval thresholds
- Amount limits
- Currency restrictions
- Country restrictions
- Merchant restrictions
- Category restrictions
- Budget limits
- Transaction limits
- Suspended and expired mandates
- Valid RSA signatures
- Tampered payment requests
- Modified nonces
- Modified idempotency keys

## Security Design

- Principals authenticate with signed JWTs.
- AI agents authenticate each payment using RSA signatures.
- Agent private keys are never stored by MandateGuard.
- Signed canonical payloads prevent request tampering.
- Nonces prevent replay attacks.
- Idempotency keys prevent duplicate processing.
- Database uniqueness constraints provide additional protection.
- PostgreSQL row locks prevent concurrent overspending.
- Audit records preserve security and decision history.
- Principal-scoped queries prevent cross-account data access.

## Current Limitation

MandateGuard currently uses a simulated payment provider. It does not move real money or connect to a production payment processor.

## Future Improvements

- Real payment-provider integrations
- Webhook notifications
- Risk scoring and anomaly detection
- Admin and risk-officer dashboards
- Key rotation
- OpenAPI documentation
- Metrics and distributed tracing
- Production database migrations
- Cloud deployment

## Author

**Qosim Faruq Olamide**

Backend Java Developer focused on secure fintech systems, payment processing and Spring Boot.