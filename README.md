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

This launcher always points PostgreSQL to the closed local port `127.0.0.1:1` and disables SMTP, so opening Swagger cannot access a configured database or send recovery email. It supplies local-only placeholder values and process-local cryptographic keys; none are saved. Use this launcher to view the API documentation and inspect schemas. Business requests require starting the API manually with a database installed from the reviewed combined SQL script, local mail settings, and a real Firebase project.

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

`local-only-db/mottainai_shared_apis_fresh_install.sql` is the single local installer for a new empty database. It combines the operational schema source with the additive client API roles, views, functions, and access rules in one transaction. It is ignored by Git and has not been executed. It has preflight checks and refuses a database that already contains the Mottainai schema or user relations. Create a disposable empty database, verify its identity and PostgreSQL version, and review the script with the DBA before applying it. Never run it against production or an existing/shared database.

The installer requires PostgreSQL 15 or newer, `pgcrypto` as `public.crypt`, and permission to transfer function and view ownership to the dedicated non-login roles. Database TLS and parameter-log redaction must also be configured before sending login or password-reset values to SQL functions. After installation, the operational and client APIs use distinct database login principals and roles against the same database.
