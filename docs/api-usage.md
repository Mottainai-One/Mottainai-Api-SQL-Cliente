# API routes and test flows

Swagger UI: `http://localhost:8081/swagger-ui/index.html` when the API runs on its default port. The OpenAPI document is available at `/v3/api-docs`.

## Authentication

Use a Firebase ID token for profile, loyalty, catalog, and geofence operations. In Swagger, authorize the `FirebaseBearer` scheme with the ID token. Do not use a Firebase refresh token or an access token from the legacy login flow for these routes.

The `/api/v1/customers/auth` routes are the separate legacy flow. `POST /login` returns the legacy bearer token used by `GET /profile`. Legacy password recovery looks up accounts in `customer_auth`; it does not reset Firebase passwords. Firebase password resets should be requested through Firebase Authentication in the app.

## Endpoint inventory

| Method | Path | Authentication | Purpose |
|---|---|---|---|
| GET | `/api/v1/client/auth/profile` | Firebase | Read the signed-in customer's profile |
| POST | `/api/v1/client/auth/profile` | Firebase | Create or resolve the signed-in customer's profile |
| PUT | `/api/v1/client/auth/profile` | Firebase | Update the required profile fields |
| GET | `/api/v1/client/loyalty/balance` | Firebase | Read the customer's points balance |
| GET | `/api/v1/client/loyalty/transactions` | Firebase | Read the paginated points history, optionally filtered by inclusive dates |
| GET | `/api/v1/customer-catalog/stores` | Firebase | Search and page through stores |
| GET | `/api/v1/customer-catalog/stores/{id}` | Firebase | Read one store |
| GET | `/api/v1/customer-catalog/promotions` | Firebase | List promotions, optionally by location and radius |
| GET | `/api/v1/customer-catalog/promotions/{id}` | Firebase | Read one promotion and its items |
| GET | `/api/v1/client/geofences` | Firebase | List the authenticated customer's geofences |
| POST | `/api/v1/client/geofences` | Firebase | Create or update the authenticated customer's geofence |
| DELETE | `/api/v1/client/geofences/{geofenceId}` | Firebase | Deactivate one of the authenticated customer's geofences |
| GET | `/api/v1/customers/{customerId}/geofences` | Firebase | List the authenticated customer's geofences |
| POST | `/api/v1/customers/{customerId}/geofences` | Firebase | Create or update a geofence |
| DELETE | `/api/v1/customers/{customerId}/geofences/{geofenceId}` | Firebase | Deactivate a geofence |
| POST | `/api/v1/customers/auth/login` | None | Sign in to a legacy customer account |
| POST | `/api/v1/customers/auth/password-recovery` | None | Request legacy password recovery |
| POST | `/api/v1/customers/auth/password-reset/validate` | None | Check a legacy password recovery token |
| POST | `/api/v1/customers/auth/password-reset` | None | Set a new password for a legacy account |
| GET | `/api/v1/customers/auth/profile` | Legacy bearer | Read the legacy account profile |

## Common request rules

- Pagination uses `page=0` and `size=20` by default. Page indices start at zero; supported page sizes are 1–100.
- Profile creation requires `fullName`, an unmasked 11-digit `cpf`, and `marketingConsent`. Phone, birth date, address, and Cloudinary `avatarUrl` are optional. If an address is supplied, its required fields must be complete.
- Firebase profile responses include full name, phone, e-mail, birth date, address, and avatar URL along with account metadata. E-mail comes from the authenticated Firebase ID token; the other private fields are read from the encrypted profile envelope.
- Profile update requires `fullName` and `marketingConsent`. Omitting `phone` or sending it blank preserves the stored phone. Omit `avatarUrl` to keep the current image; send an empty string to clear its saved URL. A non-empty avatar URL must use HTTPS and the `res.cloudinary.com` host. Clearing the URL does not delete the image asset from Cloudinary.
- The current profile `PUT` does not edit an address. Address changes need a database function update so the protected address lookup token and encrypted profile stay synchronized.
- Points history accepts optional ISO dates, for example `GET /api/v1/client/loyalty/transactions?from=2026-01-01&to=2026-01-31&page=0&size=20`. The date range includes both endpoints.
- Promotion location search requires `latitude`, `longitude`, and `radiusKm` together. The maximum search radius is 20 km. Results are ordered by distance and then discount percentage.
- Geofences currently accept radii from 50 to 10,000 metres because that is the limit enforced by the existing database. The database structure was not changed.
- Legacy reset passwords must be 8–100 characters, contain upper and lower case letters, a number, and a special character, and contain no spaces.
- Legacy password recovery returns `204` without revealing whether the account exists. An email is sent only for a matching legacy account when the recovery URL and SMTP settings are configured.

## Suggested smoke-test sequence

1. Authorize Swagger with a Firebase ID token and call `GET /api/v1/client/auth/profile`.
2. Call `GET /api/v1/client/loyalty/balance`, then `GET /api/v1/client/loyalty/transactions?page=0&size=20` (optionally with `from` and `to`).
3. Call `GET /api/v1/customer-catalog/stores?page=0&size=20`; use a returned store ID for the store detail and geofence routes.
4. Call the promotion list without location, then with `latitude`, `longitude`, and `radiusKm`.
5. Create, list, and delete a geofence through `/api/v1/client/geofences`; the Firebase token identifies the customer. The older path containing `customerId` remains available for compatibility.
6. Exercise the legacy login and password reset routes only with a dedicated legacy test account. For Firebase accounts, use Firebase Authentication's password-reset flow.

## Database permissions

The API uses the existing shared schema and database functions. Profile updates require a scoped RLS read policy for the profile function owner. Reward catalog reads are not exposed because the official database contract does not grant the client API role access to `loyalty_reward`.

