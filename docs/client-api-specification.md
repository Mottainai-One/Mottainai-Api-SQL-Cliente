# Mottainai Client API Specification

## Status

Approved scope based on the user decisions in this task. This API is a separate Spring Boot project and must not change the operational API or its shared database schema.

## Integration architecture

The client API is a separate service that connects directly to PostgreSQL through a dedicated least-privilege database role. It does not call the operational API at runtime. Its JDBC adapter uses the additive functions and filtered views proposed in `local-only-db/client-api-access-proposal.sql`. This SQL is ignored by Git and has not been applied or executed. DBA review and deployment into an isolated database are required before business routes can use it.

## Approved routes

| Method | Path | Request | Success response | Authentication and data boundary |
|---|---|---|---|---|
| GET | `/api/v1/client/auth/profile` | None | `200 CustomerResponse` | Firebase JWT; resolve customer by verified subject. |
| POST | `/api/v1/client/auth/profile` | `CreateFirebaseCustomerProfileRequest` (`fullName`, `cpf`, `phone`, `birthDate`, `marketingConsent`, `address`) | `201 CustomerResponse` | Firebase JWT; subject and email come from verified claims. |
| PUT | `/api/v1/client/auth/profile` | `UpdateFirebaseCustomerProfileRequest` (`fullName`, `phone`, `marketingConsent`) | `200 CustomerResponse` | Firebase JWT; ownership comes from verified subject. |
| GET | `/api/v1/customer-catalog/stores` | Optional `page`, `size`, `query`, `latitude`, `longitude`, `radiusKm` | `200 Page<CustomerCatalogStoreResponse>` | Firebase JWT; response contains catalog data only. |
| GET | `/api/v1/customer-catalog/stores/{id}` | None | `200 CustomerCatalogStoreResponse` | Firebase JWT; response contains catalog data only. |
| GET | `/api/v1/customer-catalog/promotions` | Optional `page`, `size`, `storeId`, `query` | `200 Page<CustomerCatalogPromotionResponse>` | Firebase JWT; response contains catalog data only. |
| GET | `/api/v1/customer-catalog/promotions/{id}` | None | `200 CustomerCatalogPromotionResponse` | Firebase JWT; response contains catalog data only. |
| GET | `/api/v1/client/loyalty/balance` | None | `200 LoyaltyAccountResponse` | Firebase JWT; customer ID is derived from verified identity. |
| POST | `/api/v1/customers/auth/login` | `CustomerLoginRequest` (`email`, `password`) | `200 CustomerTokenResponse` | Public login; verify password hash and lock state. |
| POST | `/api/v1/customers/auth/password-recovery` | `CustomerPasswordRecoveryRequest` (`email`) | `204 No Content` | Public; never reveal whether an account exists. |
| POST | `/api/v1/customers/auth/password-reset/validate` | `CustomerResetTokenRequest` (`token`) | `200 TokenValidationResponse` | Public token validation. |
| POST | `/api/v1/customers/auth/password-reset` | `CustomerPasswordResetRequest` (`token`, `newPassword`) | `204 No Content` | Public token reset; enforce the operational password rules. |
| GET | `/api/v1/customers/auth/profile` | None | `200 CustomerResponse` | Legacy customer JWT with customer role. |
| GET | `/api/v1/customers/{customerId}/geofences` | None | `200 List<CustomerGeofenceResponse>` | Legacy customer JWT; customer must own `{customerId}`. Staff-only access is outside the mobile API. |
| POST | `/api/v1/customers/{customerId}/geofences` | `CreateCustomerGeofenceRequest` (`storeId`, `radiusMeters`) | `201 CustomerGeofenceResponse` | Customer JWT; customer must own `{customerId}`. |
| DELETE | `/api/v1/customers/{customerId}/geofences/{geofenceId}` | None | `204 No Content` | Customer JWT; customer must own `{customerId}` and the geofence. |

## Route traceability matrix

| Approved route | New controller and operation | Existing rule/source | New contract/model | Database objects in the local proposal |
|---|---|---|---|---|
| `GET /api/v1/client/auth/profile` | `CustomerProfileController.profile` → `ClientApiOperations.getFirebaseProfile` | `CustomerService.getFirebaseProfile`; return the existing active Firebase customer's own profile | `CustomerResponse` | `fn_bootstrap_customer_context`, `customer`, `api_client_customer_private_data` |
| `POST /api/v1/client/auth/profile` | `CustomerProfileController.createProfile` → `createFirebaseProfile` | `CustomerService.createFirebaseProfile`; idempotent same identity, reject inactive linked identities and duplicate CPF/email | `CreateFirebaseCustomerProfileRequest`, `CustomerResponse`, encrypted private-data envelope | `fn_api_reserve_customer_id`, `fn_api_create_firebase_customer_profile`, `customer`, `customer_private_data`, `loyalty_account` |
| `PUT /api/v1/client/auth/profile` | `CustomerProfileController.updateProfile` → `updateFirebaseProfile` | `CustomerService.updateFirebaseProfile`; only name, phone, marketing consent change | `UpdateFirebaseCustomerProfileRequest`, `CustomerResponse` | `fn_bootstrap_customer_context`, `fn_api_update_firebase_customer_profile`, `customer`, `customer_private_data` |
| `GET /api/v1/customer-catalog/stores` | `CustomerCatalogController.stores` → `listStores` | `CustomerCatalogService`; active/non-deleted filter, paging, text and optional complete geo filter | `CustomerStoreCatalogQuery`, `PageResponse<CustomerCatalogStoreResponse>` | `api_client_catalog_stores` view over `retail_store`, `address` |
| `GET /api/v1/customer-catalog/stores/{id}` | `CustomerCatalogController.store` → `getStore` | `CustomerCatalogService.getStore`; same visibility filter as list | `CustomerCatalogStoreResponse` | `api_client_catalog_stores`, `retail_store`, `address` |
| `GET /api/v1/customer-catalog/promotions` | `CustomerCatalogController.promotions` → `listPromotions` | `CustomerCatalogService`; approved/current promotion, valid store/product/items and stable ordering | `CustomerPromotionCatalogQuery`, `PageResponse<CustomerCatalogPromotionResponse>` | `api_client_catalog_promotions` view over `promotion`, `promotion_item`, `product`, `retail_store`, `address` |
| `GET /api/v1/customer-catalog/promotions/{id}` | `CustomerCatalogController.promotion` → `getPromotion` | `CustomerCatalogService.getPromotion`; same eligibility criteria as list | `CustomerCatalogPromotionResponse` and nested `Item` | `api_client_catalog_promotions`, `api_client_catalog_promotion_items` |
| `GET /api/v1/client/loyalty/balance` | `ClientLoyaltyController.balance` → `getLoyaltyBalance` | `LoyaltyService`; authenticated customer's current point balance only | `LoyaltyAccountResponse` | `fn_bootstrap_customer_context`, `loyalty_account` under customer RLS |
| `POST /api/v1/customers/auth/login` | `CustomerAuthenticationController.login` → `login` | `CustomerAuthenticationService`; five failed attempts lock for 30 minutes; generic invalid-credential response | `CustomerLoginRequest`, `CustomerTokenResponse` | `fn_api_customer_login`, `customer_auth`, `customer` |
| `POST /api/v1/customers/auth/password-recovery` | `CustomerAuthenticationController.requestPasswordRecovery` → `requestPasswordRecovery` | `CustomerAuthenticationService`; generic 204, one-time token expires in 15 minutes, send after commit | `CustomerPasswordRecoveryRequest` | `fn_api_request_customer_recovery`, `fn_api_clear_customer_recovery`, `customer_auth`, `customer` |
| `POST /api/v1/customers/auth/password-reset/validate` | `CustomerAuthenticationController.validatePasswordReset` → `validatePasswordReset` | `CustomerAuthenticationService`; check one-time token and expiry without exposing account data | `CustomerResetTokenRequest`, `TokenValidationResponse` | `fn_api_validate_customer_recovery`, `customer_auth`, `customer` |
| `POST /api/v1/customers/auth/password-reset` | `CustomerAuthenticationController.resetPassword` → `resetPassword` | `CustomerAuthenticationService`; password strength, token expiry, clear token/lock/failure count | `CustomerPasswordResetRequest` | `fn_api_reset_customer_password`, `customer_auth`, `customer` |
| `GET /api/v1/customers/auth/profile` | `CustomerAuthenticationController.profile` → `getLegacyProfile` | `CustomerAuthenticationService`; legacy customer JWT and existing limited profile response | `CustomerResponse` | `fn_api_bootstrap_legacy_customer_context`, `customer`, `customer_auth` |
| `GET /api/v1/customers/{customerId}/geofences` | `CustomerGeofenceController.list` → `listGeofences` | `CustomerController`; customer role only in the new mobile boundary; authenticated ID must own path ID | `CustomerGeofenceResponse` | `fn_api_bootstrap_legacy_customer_context`, `customer_geofence`, `api_client_catalog_stores` |
| `POST /api/v1/customers/{customerId}/geofences` | `CustomerGeofenceController.add` → `addGeofence` | `CustomerService`; owner check, store must be visible, radius 50–10,000 metres, existing pair is reactivated/updated | `CreateCustomerGeofenceRequest`, `CustomerGeofenceResponse` | `fn_api_bootstrap_legacy_customer_context`, `customer_geofence`, `api_client_catalog_stores` |
| `DELETE /api/v1/customers/{customerId}/geofences/{geofenceId}` | `CustomerGeofenceController.remove` → `removeGeofence` | `CustomerService`; owner check and logical deactivation only | No-content response | `fn_api_bootstrap_legacy_customer_context`, `customer_geofence` |

## Explicit exclusions

- `GET /api/v1/client/loyalty/transactions` (removed from the approved scope by the user's later instruction)
- `POST /api/v1/client/loyalty/redeem` (removed from the approved scope by the user's later instruction)
- Any reward catalog, reward redemption, or earn route
- Administrative `POST /api/v1/customers`; customer onboarding is the Firebase profile POST above

## Behavior and validation rules

- Store lists default to `page=0`, `size=20`; valid page is nonnegative, size is 1 through 100, and query length is at most 100 characters. Results sort by store name ascending, then ID ascending. Latitude, longitude, and radius must be supplied together; latitude is within [-90, 90], longitude within [-180, 180], and radius is finite and greater than 0 and at most 500 km.
- Store catalog results and details exclude inactive and soft-deleted stores.
- Promotion catalog results and details include only approved, active, non-deleted promotions with `startsAt <= now < endsAt`, an active non-deleted store, and at least one item with positive available quantity, no deletion timestamp, and an active non-deleted product. Lists sort by start time descending, then ID descending.
- Legacy login preserves the operational five-failure, 30-minute lockout. Password recovery uses a 15-minute token and a generic response. Password reset requires at least eight characters with upper- and lower-case letters, a digit, and a special character, then clears the recovery token, lock, and failure count.
- Geofence radius must remain within the database range of 50 to 10,000 metres. Customer identity is checked against the authenticated principal; path IDs are never sufficient authorization.
- Geofence access in the new mobile API is customer-only. The operational API also permits staff roles on the legacy GET route, but staff access is outside the approved client API boundary.
- Firebase profile creation is idempotent for the same active Firebase identity and rejects an identity linked to an inactive customer. CPF or email collision with another identity returns conflict. Firebase PUT updates only name, phone, and marketing consent.
- The legacy customer profile response intentionally contains only `id`, `birthYear`, `active`, `marketingConsent`, `createdAt`, and `updatedAt`.
- `LoyaltyAccountResponse` contains only `pointsBalance`. `CustomerCatalogStoreResponse` contains `id`, `name`, `address`, `latitude`, and `longitude`. `CustomerCatalogPromotionResponse` contains `id`, `name`, `description`, `promotionType`, `startsAt`, `endsAt`, `store`, and customer-safe item fields (`id`, `productId`, `name`, `originalPrice`, `promotionalPrice`, `quantityAvailable`).

## Traceability evidence

Operational source repository (reference only): `Interdisciplinar/Projeto-API-Operacional/Mottainai-Spring-Api-SQL`.

| Area | Operational definitions | Database definitions in `mottainai_operational_complete.sql` |
|---|---|---|
| Firebase profile | `controller/CustomerProfileController.java`; `service/CustomerService.java`; `service/CustomerAuthenticationService.java`; `service/CustomerPrivateDataService.java`; `security/CustomerDataCryptography.java`; `security/SecurityConfig.java` | `customer` 1280–1294; `customer_auth` 1296–1312; encrypted `customer_private_data` 6827–6891; customer-private-data RLS 6964–6987 |
| Legacy login and password recovery | `controller/CustomerAuthenticationController.java`; `service/CustomerAuthenticationService.java`; `security/JwtService.java` | `customer_auth` 1296–1312; token/hash constraints 4766–4771 |
| Legacy profile | `controller/CustomerProfileController.java`; `service/CustomerAuthenticationService.java`; `dto/response/CustomerResponse.java` | customer and customer-auth records above |
| Store catalog | `controller/CustomerCatalogController.java`; `service/CustomerCatalogService.java`; `repository/RetailStoreRepository.java`; `dto/response/CustomerCatalogStoreResponse.java` | `retail_store` 866–880 |
| Promotion catalog | `controller/CustomerCatalogController.java`; `service/CustomerCatalogService.java`; `repository/PromotionRepository.java`; `repository/PromotionItemRepository.java`; `dto/response/CustomerCatalogPromotionResponse.java` | `promotion` 1479–1497; `promotion_item` 1499–1514 |
| Loyalty balance | `controller/LoyaltyController.java`; `service/LoyaltyService.java`; `service/CustomerAccess.java` | `loyalty_account` 1527–1534; customer context 4871–4910; loyalty RLS 4939–4966 |
| Geofences | `controller/CustomerController.java`; `service/CustomerService.java`; `service/CustomerAccess.java`; `model/CustomerGeofence.java` | `customer_geofence` 1516–1525; customer RLS 4927–4931 |

The operational API documentation `docs/CUSTOMER_MOBILE_API.md` supplies the mobile profile payload and catalog/loyalty descriptions. The two other operational documents list transaction and redeem routes, but those routes were explicitly removed from this scope. No live database was contacted and no SQL was executed.

## Isolation and data protection

- Implement only in this client API repository. Do not edit, import as a runtime module, or change the operational project.
- Do not return protected database tokens, password hashes, recovery-token hashes, Firebase identity tokens, encrypted payloads, or decryption keys.
- Customer private data is encrypted using the operational AES-GCM envelope. Keys must come from external configuration; no key or environment file is committed.
- The SQL role `mottainai_customer_api` is `NOLOGIN`, `NOINHERIT`, and does not bypass RLS (SQL 4834–4849). Its grants omit catalog tables (4968–4977), customer INSERT (4912–4925), and pre-authentication customer lookup; `fn_bootstrap_customer_context` only resolves an already-existing protected Firebase identity (4879–4910). A dedicated API role and narrow database operations must be approved by the DBA; no broad operational database role may be used.
- The local proposal adds `promotion_item.deleted_at` because the operational promotion-item repository filters on that field while the supplied consolidated schema omits it. This is a proposed additive schema change and must be reviewed before applying it to an isolated database. No database was contacted and no SQL was executed.
- Before deployment, verify PostgreSQL support for `GRANT SET ON PARAMETER`, the installed schema for `pgcrypto`, ownership/membership privileges, TLS, and SQL parameter-log redaction. Validate the proposal against a disposable database copy before connecting the API.
