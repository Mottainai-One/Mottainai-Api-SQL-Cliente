# Mottainai Customer API

Spring Boot REST API for the Mottainai mobile app. It exposes customer profile, loyalty, store and promotion catalog, and geofence operations over the shared PostgreSQL database.

## Capabilities

- Firebase authenticated profile, loyalty, catalog, and geofence endpoints.
- Legacy customer login and password recovery endpoints for accounts stored in the legacy authentication table.
- OpenAPI documentation and a local Swagger launcher.

Firebase and legacy authentication are separate flows. New app accounts should use Firebase Authentication. The legacy password recovery endpoint only handles legacy database accounts; Firebase password resets belong to the Firebase client SDK.

## Requirements

- Java 21
- Maven 3.9 or newer
- A PostgreSQL database with the shared Mottainai schema, functions, views, and required grants installed separately
- A Firebase project for Firebase protected endpoints

The API repository does not create or alter the shared database schema. Customer profile updates require the scoped RLS permissions described in the deployment notes.

## Configuration

Set the following environment variables before starting the application:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection |
| `FIREBASE_PROJECT_ID` | Firebase token verification |
| `JWT_SECRET`, `JWT_ISSUER` | Legacy customer access tokens |
| `CUSTOMER_DATA_ENCRYPTION_KEY` | Encryption key for protected profile payloads |

Optional SMTP settings are needed only for legacy customer password recovery: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, and `CUSTOMER_PASSWORD_RECOVERY_URL`. The endpoint returns `204` even when no matching legacy account exists or mail configuration is incomplete.

## Run locally

```powershell
mvn spring-boot:run
```

Swagger UI is available at `/swagger-ui/index.html`; the OpenAPI document is at `/v3/api-docs`. The `scripts/run-swagger.ps1` helper starts a documentation-only local setup. It does not provide a configured database, Firebase project, or SMTP server.

See [API routes and test flows](docs/api-usage.md) for endpoint authentication and request examples.
