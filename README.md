# Mottainai Client API

Standalone Spring Boot REST API for the Mottainai customer mobile application. The service connects directly to PostgreSQL using a dedicated least-privilege application role; it does not proxy requests through the operational API.

## Requirements

- JDK 21
- Apache Maven 3.9.16 or newer
- An isolated PostgreSQL database based on the operational schema for business-route testing
- A Firebase project for Firebase-protected route testing

The API expects these environment variables when connecting to PostgreSQL:

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` | Dedicated server-side API role |
| `DB_PASSWORD` | Credential stored outside the repository |
| `FIREBASE_PROJECT_ID` | Firebase project used to validate ID tokens |
| `JWT_SECRET` | Base64-encoded secret containing at least 32 random bytes |
| `JWT_ISSUER` | Issuer claim for customer legacy JWTs |
| `CUSTOMER_DATA_ENCRYPTION_KEY` | Base64-encoded 32-byte AES key |
| `STAFF_DATA_ENCRYPTION_KEY` | Separate Base64-encoded 32-byte staff key used for separation validation |
| `CUSTOMER_PASSWORD_RECOVERY_URL` | Optional customer-app reset URL used in recovery email |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Optional SMTP settings for recovery email delivery |

Do not commit credentials or production data. Keep the customer and staff encryption keys distinct.

## Start the API and open Swagger UI

In PowerShell, from the project root, run:

```powershell
.\scripts\run-swagger.ps1
```

This launcher always points PostgreSQL to the closed local port `127.0.0.1:1` and disables SMTP, so opening Swagger cannot access a configured database or send recovery email. It supplies local-only placeholder values and process-local cryptographic keys; none are saved. Use this launcher to view the API documentation and inspect schemas. Business requests require starting the API manually with a verified disposable PostgreSQL database, the reviewed local database proposal, local mail settings, and a real Firebase project.

Open:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

To try protected routes, click **Authorize** in Swagger UI and supply a Firebase ID token or a customer legacy access token, as indicated by the endpoint. Login and password recovery endpoints are public.

Password recovery returns a generic response whether or not the account exists. The documentation-only launcher disables recovery delivery. For business testing, configure SMTP and a reset URL; if email delivery fails, the API attempts to clear the newly issued token.

## Build and tests

```powershell
mvn test
```

The automated tests verify controller route mappings and configured bearer schemes. A real PostgreSQL clone is required to validate database queries and end-to-end business requests; these tests do not connect to PostgreSQL.

## Database setup

`local-only-db/client-api-access-proposal.sql` is a local review artifact and is ignored by Git. It has not been executed. Before business-route testing, prepare a disposable PostgreSQL copy, verify its identity and version, review the proposal with the database owner, and apply it only to that isolated copy. Never apply the proposal to a production or shared database from this project.

The proposal assumes `pgcrypto` is available as `public.crypt`, supports `GRANT SET ON PARAMETER`, and requires the deployer to transfer function and view ownership to the dedicated non-login roles. Database TLS and parameter-log redaction must also be configured before sending login or password-reset values to SQL functions.
