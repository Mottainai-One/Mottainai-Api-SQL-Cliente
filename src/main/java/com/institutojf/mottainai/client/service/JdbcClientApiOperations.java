package com.institutojf.mottainai.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.institutojf.mottainai.client.dto.request.CreateAddressRequest;
import com.institutojf.mottainai.client.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.client.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.request.CustomerLoginRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordRecoveryRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordResetRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPromotionCatalogQuery;
import com.institutojf.mottainai.client.dto.request.CustomerResetTokenRequest;
import com.institutojf.mottainai.client.dto.request.CustomerStoreCatalogQuery;
import com.institutojf.mottainai.client.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.response.AddressResponse;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogPromotionResponse;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogStoreResponse;
import com.institutojf.mottainai.client.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.dto.response.FirebaseCustomerProfileResponse;
import com.institutojf.mottainai.client.dto.response.CustomerTokenResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.client.dto.response.CustomerLoyaltyRewardResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyTransactionResponse;
import com.institutojf.mottainai.client.dto.response.PageResponse;
import com.institutojf.mottainai.client.dto.response.TokenValidationResponse;
import com.institutojf.mottainai.client.security.CustomerDataCryptography;
import com.institutojf.mottainai.client.security.LegacyCustomerTokenService;
import com.institutojf.mottainai.client.security.ProtectedDataToken;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDBC implementation against the narrow PostgreSQL functions and views in the local DBA proposal. */
@Service
public final class JdbcClientApiOperations implements ClientApiOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcClientApiOperations.class);
    private static final String CUSTOMER_COLUMNS =
            "customer_id, birth_year, active, marketing_consent, created_at, updated_at";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final RowMapper<CustomerResponse> CUSTOMER_MAPPER = (rs, row) -> customer(rs);
    private static final RowMapper<CustomerCatalogStoreResponse> STORE_MAPPER = JdbcClientApiOperations::store;
    private static final RowMapper<CustomerGeofenceResponse> GEOFENCE_MAPPER = JdbcClientApiOperations::geofence;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final CustomerDataCryptography cryptography;
    private final LegacyCustomerTokenService legacyTokens;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String recoveryUrl;
    private final String mailFrom;

    public JdbcClientApiOperations(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            CustomerDataCryptography cryptography,
            LegacyCustomerTokenService legacyTokens,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${security.customer.password-recovery.url:}") String recoveryUrl,
            @Value("${spring.mail.username:}") String mailFrom) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.cryptography = cryptography;
        this.legacyTokens = legacyTokens;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
        this.recoveryUrl = recoveryUrl;
        this.mailFrom = mailFrom;
    }

    @Override
    public FirebaseCustomerProfileResponse getFirebaseProfile(String firebaseUid, String firebaseEmail) {
        return transactions.execute(status -> {
            Integer id = bootstrapFirebase(firebaseUid);
            return firebaseProfile(selectCustomer(id), firebaseEmail);
        });
    }

    @Override
    public FirebaseCustomerProfileResponse createFirebaseProfile(
            CreateFirebaseCustomerProfileRequest request, String firebaseUid, String email) {
        return transactions.execute(status -> {
            Integer id = jdbc.queryForObject("SELECT mottainai.fn_api_reserve_customer_id()", Integer.class);
            String protectedName = ProtectedDataToken.customerName(request.fullName());
            String protectedCpf = ProtectedDataToken.cpf(request.cpf());
            String protectedEmail = ProtectedDataToken.email(email);
            String protectedPhone = ProtectedDataToken.phone(request.phone());
            String protectedUid = ProtectedDataToken.externalAuthUid(firebaseUid);
            String protectedAddress = addressToken(request.address());
            validateCloudinaryUrl(request.avatarUrl());
            Short birthYear = null;
            if (request.birthDate() != null) {
                int year = request.birthDate().getYear();
                if (year < 1900 || year > LocalDate.now().getYear()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Birth date is outside the supported range");
                }
                birthYear = (short) year;
            }
            CustomerDataCryptography.Envelope envelope = cryptography.encrypt(id, profilePayload(
                    request.fullName(), request.cpf(), email, request.phone(), request.birthDate(),
                    request.address(), request.avatarUrl()));
            ProfileResult result = jdbc.queryForObject(
                    "SELECT * FROM mottainai.fn_api_create_firebase_customer_profile(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    (rs, row) -> new ProfileResult(rs.getString("result_code"), customer(rs)),
                    id, protectedName, protectedCpf, protectedEmail, protectedPhone, protectedAddress,
                    protectedUid, birthYear, request.marketingConsent(), envelope.ciphertext(), envelope.nonce(),
                    envelope.keyVersion(), Timestamp.valueOf(LocalDateTime.now()));
            if (result == null) {
                throw new IllegalStateException("The database did not return a Firebase profile result");
            }
            return firebaseProfile(profileResult(result, "CREATED", "EXISTING_ACTIVE"), email);
        });
    }

    @Override
    public FirebaseCustomerProfileResponse updateFirebaseProfile(
            UpdateFirebaseCustomerProfileRequest request, String firebaseUid, String firebaseEmail) {
        return transactions.execute(status -> {
            Integer id = bootstrapFirebase(firebaseUid);
            PrivateEnvelope stored = readEnvelope(id);
            JsonNode prior;
            try {
                prior = objectMapper.readTree(cryptography.decrypt(id,
                        new CustomerDataCryptography.Envelope(stored.ciphertext, stored.nonce, stored.keyVersion)));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored customer personal data is not valid JSON", exception);
            }
            ObjectNode updated = prior != null && prior.isObject()
                    ? ((ObjectNode) prior).deepCopy() : objectMapper.createObjectNode();
            updated.put("fullName", request.fullName());
            String phone = request.phone();
            if (phone != null && !phone.isBlank()) updated.put("phone", phone);
            if (request.avatarUrlSpecified()) {
                validateCloudinaryUrl(request.avatarUrl());
                if (request.avatarUrl() == null || request.avatarUrl().isBlank()) updated.putNull("avatarUrl");
                else updated.put("avatarUrl", request.avatarUrl().trim());
            }
            String preservedPhone = updated.path("phone").isTextual() ? updated.path("phone").asText() : null;
            String serialized = writeJson(updated);
            CustomerDataCryptography.Envelope envelope = cryptography.encrypt(id, serialized);
            ProfileResult result = jdbc.queryForObject(
                    "SELECT * FROM mottainai.fn_api_update_firebase_customer_profile(?,?,?,?,?,?,?,?)",
                    (rs, row) -> new ProfileResult(rs.getString("result_code"), customer(rs)),
                    ProtectedDataToken.externalAuthUid(firebaseUid), ProtectedDataToken.customerName(request.fullName()),
                    ProtectedDataToken.phone(preservedPhone), request.marketingConsent(), envelope.ciphertext(),
                    envelope.nonce(), envelope.keyVersion(), Timestamp.valueOf(LocalDateTime.now()));
            if (result == null) throw new IllegalStateException("The database did not return a profile update result");
            return firebaseProfile(profileResult(result, "UPDATED"), firebaseEmail);
        });
    }

    @Override
    public PageResponse<CustomerCatalogStoreResponse> listStores(CustomerStoreCatalogQuery query) {
        int page = query.resolvedPage();
        int size = query.resolvedSize();
        String text = blankToNull(query.query());
        boolean geo = query.latitude() != null;
        StringBuilder predicate = new StringBuilder(" WHERE TRUE");
        List<Object> args = new ArrayList<>();
        if (text != null) {
            predicate.append(" AND lower(name) LIKE lower(?)");
            args.add("%" + text + "%");
        }
        if (geo) {
            predicate.append(" AND (6371.0088 * acos(least(1.0, greatest(-1.0, "
                    + "sin(radians(CAST(? AS double precision))) * sin(radians(latitude)) + "
                    + "cos(radians(CAST(? AS double precision))) * cos(radians(latitude)) * "
                    + "cos(radians(longitude) - radians(CAST(? AS double precision))))))) <= ?");
            args.add(query.latitude()); args.add(query.latitude()); args.add(query.longitude()); args.add(query.radiusKm());
        }
        long total = count("SELECT count(*) FROM mottainai.api_client_catalog_stores" + predicate, args);
        args.add(size); args.add((long) page * size);
        List<CustomerCatalogStoreResponse> content = jdbc.query(
                "SELECT * FROM mottainai.api_client_catalog_stores" + predicate
                        + " ORDER BY name ASC, id ASC LIMIT ? OFFSET ?", STORE_MAPPER, args.toArray());
        return page(content, page, size, total);
    }

    @Override
    public CustomerCatalogStoreResponse getStore(Integer storeId) {
        return queryOne("SELECT * FROM mottainai.api_client_catalog_stores WHERE id = ?", STORE_MAPPER, storeId,
                "Store was not found");
    }

    @Override
    public PageResponse<CustomerCatalogPromotionResponse> listPromotions(CustomerPromotionCatalogQuery query) {
        int page = query.resolvedPage();
        int size = query.resolvedSize();
        String text = blankToNull(query.query());
        StringBuilder predicate = new StringBuilder(" WHERE TRUE");
        List<Object> args = new ArrayList<>();
        if (query.storeId() != null) {
            predicate.append(" AND p.store_id = ?");
            args.add(query.storeId());
        }
        if (text != null) {
            predicate.append(" AND lower(p.name) LIKE lower(?)");
            args.add("%" + text + "%");
        }
        String distanceExpression = "(6371.0088 * acos(least(1.0, greatest(-1.0, "
                + "sin(radians(CAST(? AS double precision))) * sin(radians(p.latitude)) + "
                + "cos(radians(CAST(? AS double precision))) * cos(radians(p.latitude)) * "
                + "cos(radians(p.longitude) - radians(CAST(? AS double precision)))))))";
        boolean geographicSearch = query.latitude() != null;
        if (geographicSearch) {
            predicate.append(" AND ").append(distanceExpression).append(" <= ?");
            args.add(query.latitude());
            args.add(query.latitude());
            args.add(query.longitude());
            args.add(query.radiusKm());
        }
        long total = count("SELECT count(*) FROM mottainai.api_client_catalog_promotions p" + predicate,
                args);
        String selectedDistance = geographicSearch ? distanceExpression : "NULL::double precision";
        List<Object> contentArgs = new ArrayList<>();
        if (geographicSearch) {
            contentArgs.add(query.latitude());
            contentArgs.add(query.latitude());
            contentArgs.add(query.longitude());
        }
        contentArgs.addAll(args);
        contentArgs.add(size);
        contentArgs.add((long) page * size);
        List<PromotionSummary> summaries = jdbc.query(
                "SELECT p.id, " + selectedDistance + " AS distance_km, "
                        + "COALESCE(discount.discount_percentage, 0) AS discount_percentage "
                        + "FROM mottainai.api_client_catalog_promotions p "
                        + "LEFT JOIN (SELECT promotion_id, MAX(CASE WHEN original_price > 0 "
                        + "THEN ROUND((original_price - promotional_price) * 100.0 / original_price, 2) "
                        + "ELSE 0 END) AS discount_percentage "
                        + "FROM mottainai.api_client_catalog_promotion_items GROUP BY promotion_id) discount "
                        + "ON discount.promotion_id = p.id" + predicate
                        + " ORDER BY distance_km ASC NULLS LAST, discount_percentage DESC, "
                        + "p.starts_at DESC, p.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> {
                    Double distance = (Double) rs.getObject("distance_km");
                    return new PromotionSummary(rs.getInt("id"),
                            distance == null ? null : BigDecimal.valueOf(distance).setScale(3, java.math.RoundingMode.HALF_UP),
                            rs.getBigDecimal("discount_percentage"));
                }, contentArgs.toArray());
        List<CustomerCatalogPromotionResponse> content = summaries.stream().map(summary -> {
            CustomerCatalogPromotionResponse promotion = getPromotion(summary.id());
            return new CustomerCatalogPromotionResponse(promotion.id(), promotion.name(), promotion.description(),
                    promotion.promotionType(), promotion.startsAt(), promotion.endsAt(), promotion.store(),
                    summary.distanceKm(), summary.discountPercentage(), promotion.items());
        }).toList();
        return page(content, page, size, total);
    }

    @Override
    public CustomerCatalogPromotionResponse getPromotion(Integer promotionId) {
        PromotionBase base = queryOne(
                "SELECT * FROM mottainai.api_client_catalog_promotions WHERE id = ?",
                (rs, row) -> new PromotionBase(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                        rs.getString("promotion_type"), localDateTime(rs, "starts_at"), localDateTime(rs, "ends_at"),
                        store(rs, row)), promotionId, "Promotion was not found");
        List<CustomerCatalogPromotionResponse.Item> items = jdbc.query(
                "SELECT id, product_id, name, original_price, promotional_price, quantity_available "
                        + "FROM mottainai.api_client_catalog_promotion_items WHERE promotion_id = ? ORDER BY id",
                (rs, row) -> new CustomerCatalogPromotionResponse.Item(rs.getInt("id"), rs.getInt("product_id"),
                        rs.getString("name"), rs.getBigDecimal("original_price"),
                        rs.getBigDecimal("promotional_price"), rs.getBigDecimal("quantity_available")), promotionId);
        BigDecimal discountPercentage = items.stream()
                .filter(item -> item.originalPrice() != null && item.originalPrice().signum() > 0
                        && item.promotionalPrice() != null)
                .map(item -> item.originalPrice().subtract(item.promotionalPrice())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(item.originalPrice(), 2, java.math.RoundingMode.HALF_UP))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO.setScale(2));
        return new CustomerCatalogPromotionResponse(base.id, base.name, base.description, base.type,
                base.startsAt, base.endsAt, base.store, null, discountPercentage, items);
    }

    @Override
    public LoyaltyAccountResponse getLoyaltyBalance(String firebaseUid) {
        return transactions.execute(status -> {
            Integer id = bootstrapFirebase(firebaseUid);
            Integer points;
            try {
                points = jdbc.queryForObject(
                        "SELECT points_balance FROM mottainai.loyalty_account WHERE customer_id = ? AND active IS TRUE",
                        Integer.class, id);
            } catch (EmptyResultDataAccessException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loyalty account was not found");
            }
            return new LoyaltyAccountResponse(points);
        });
    }

    @Override
    public List<CustomerLoyaltyRewardResponse> getLoyaltyRewards(String firebaseUid) {
        return transactions.execute(status -> {
            Integer customerId = bootstrapFirebase(firebaseUid);
            Integer points;
            try {
                points = jdbc.queryForObject("SELECT points_balance FROM mottainai.loyalty_account "
                        + "WHERE customer_id = ? AND active IS TRUE", Integer.class, customerId);
            } catch (EmptyResultDataAccessException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loyalty account was not found");
            }
            return jdbc.query("SELECT reward_id, name, description, points_cost "
                            + "FROM mottainai.loyalty_reward WHERE active IS TRUE "
                            + "AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP) "
                            + "AND (valid_until IS NULL OR valid_until > CURRENT_TIMESTAMP) "
                            + "ORDER BY points_cost ASC, name ASC, reward_id ASC",
                    (rs, row) -> {
                        int cost = rs.getInt("points_cost");
                        return new CustomerLoyaltyRewardResponse(rs.getInt("reward_id"), rs.getString("name"),
                                rs.getString("description"), cost, points >= cost, Math.max(0, cost - points));
                    });
        });
    }

    @Override
    public PageResponse<LoyaltyTransactionResponse> getLoyaltyTransactions(
            String firebaseUid, int page, int size, LocalDate from, LocalDate to) {
        return transactions.execute(status -> {
            Integer customerId = bootstrapFirebase(firebaseUid);
            Integer loyaltyAccountId;
            try {
                loyaltyAccountId = jdbc.queryForObject(
                        "SELECT loyalty_account_id FROM mottainai.loyalty_account "
                                + "WHERE customer_id = ? AND active IS TRUE",
                        Integer.class, customerId);
            } catch (EmptyResultDataAccessException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loyalty account was not found");
            }
            StringBuilder where = new StringBuilder(" WHERE loyalty_account_id = ?");
            List<Object> args = new ArrayList<>();
            args.add(loyaltyAccountId);
            if (from != null) {
                where.append(" AND COALESCE(sale_date, created_at) >= ?");
                args.add(Timestamp.valueOf(from.atStartOfDay()));
            }
            if (to != null) {
                where.append(" AND COALESCE(sale_date, created_at) < ?");
                args.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            }
            long total = count("SELECT count(loyalty_transaction_id) FROM mottainai.loyalty_transaction "
                    + where, args);
            args.add(size);
            args.add((long) page * size);
            List<LoyaltyTransactionResponse> content = jdbc.query(
                    "SELECT loyalty_transaction_id, sale_id, sale_date, transaction_type, points, description, created_at "
                            + "FROM mottainai.loyalty_transaction " + where
                            + " ORDER BY created_at DESC, loyalty_transaction_id DESC LIMIT ? OFFSET ?",
                    (rs, row) -> new LoyaltyTransactionResponse(rs.getInt("loyalty_transaction_id"),
                            (Integer) rs.getObject("sale_id"), localDateTime(rs, "sale_date"),
                            rs.getString("transaction_type"), rs.getInt("points"), rs.getString("description"),
                            localDateTime(rs, "created_at")),
                    args.toArray());
            return page(content, page, size, total);
        });
    }

    @Override
    public CustomerTokenResponse login(CustomerLoginRequest request) {
        String protectedEmail = ProtectedDataToken.email(request.email());
        Integer customerId = jdbc.queryForObject(
                "SELECT mottainai.fn_api_customer_login(?,?,?)", Integer.class,
                protectedEmail, request.password(), Timestamp.valueOf(LocalDateTime.now()));
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid customer credentials or account is locked");
        }
        return new CustomerTokenResponse(legacyTokens.generateAccessToken(protectedEmail), "Bearer",
                legacyTokens.expiresInSeconds());
    }

    @Override
    public void requestPasswordRecovery(CustomerPasswordRecoveryRequest request) {
        if (recoveryUrl.isBlank() || mailFrom.isBlank() || mailSender.getIfAvailable() == null) {
            return;
        }
        String emailToken = ProtectedDataToken.email(request.email());
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String secretHash = passwordEncoder.encode(secret);
        Integer authId = jdbc.queryForObject("SELECT mottainai.fn_api_request_customer_recovery(?,?,?)",
                Integer.class, emailToken, secretHash, Timestamp.valueOf(LocalDateTime.now()));
        if (authId != null) {
            String token = authId + "." + secret;
            Runnable send = () -> sendRecoveryEmail(authId, request.email(), token, secret);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { send.run(); }
                });
            } else send.run();
        }
    }

    @Override
    public TokenValidationResponse validatePasswordReset(CustomerResetTokenRequest request) {
        RecoveryToken token = parseRecoveryToken(request.token());
        Boolean valid = jdbc.queryForObject("SELECT mottainai.fn_api_validate_customer_recovery(?,?,?)",
                Boolean.class, token.customerAuthId, token.secret, Timestamp.valueOf(LocalDateTime.now()));
        return new TokenValidationResponse(Boolean.TRUE.equals(valid));
    }

    @Override
    public void resetPassword(CustomerPasswordResetRequest request) {
        if (!request.newPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,100}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must include upper and lower case letters, a digit, and a special character");
        }
        RecoveryToken token = parseRecoveryToken(request.token());
        Boolean reset = jdbc.queryForObject("SELECT mottainai.fn_api_reset_customer_password(?,?,?,?)",
                Boolean.class, token.customerAuthId, token.secret, passwordEncoder.encode(request.newPassword()),
                Timestamp.valueOf(LocalDateTime.now()));
        if (!Boolean.TRUE.equals(reset)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token is invalid or expired");
        }
    }

    @Override
    public CustomerResponse getLegacyProfile(String protectedLoginEmail) {
        return transactions.execute(status -> {
            Integer id = bootstrapLegacy(protectedLoginEmail);
            return selectCustomer(id);
        });
    }

    @Override
    public List<CustomerGeofenceResponse> listMyGeofences(String firebaseUid) {
        return transactions.execute(status -> queryGeofences(bootstrapFirebase(firebaseUid)));
    }

    @Override
    public CustomerGeofenceResponse addMyGeofence(CreateCustomerGeofenceRequest request, String firebaseUid) {
        return transactions.execute(status -> addGeofenceForCustomer(bootstrapFirebase(firebaseUid), request));
    }

    @Override
    public void removeMyGeofence(Integer geofenceId, String firebaseUid) {
        transactions.executeWithoutResult(status -> removeGeofenceForCustomer(
                bootstrapFirebase(firebaseUid), geofenceId));
    }

    @Override
    public List<CustomerGeofenceResponse> listGeofences(Integer customerId, String firebaseUid) {
        return transactions.execute(status -> {
            requireFirebaseOwner(customerId, firebaseUid);
            return queryGeofences(customerId);
        });
    }

    @Override
    public CustomerGeofenceResponse addGeofence(
            Integer customerId, CreateCustomerGeofenceRequest request, String firebaseUid) {
        return transactions.execute(status -> {
            requireFirebaseOwner(customerId, firebaseUid);
            return addGeofenceForCustomer(customerId, request);
        });
    }

    @Override
    public void removeGeofence(Integer customerId, Integer geofenceId, String firebaseUid) {
        transactions.executeWithoutResult(status -> {
            requireFirebaseOwner(customerId, firebaseUid);
            removeGeofenceForCustomer(customerId, geofenceId);
        });
    }

    private List<CustomerGeofenceResponse> queryGeofences(Integer customerId) {
        return jdbc.query("SELECT g.geofence_id, g.customer_id, g.store_id, s.name AS store_name, "
                        + "g.radius_meters, g.active, g.created_at FROM mottainai.customer_geofence g "
                        + "JOIN mottainai.api_client_catalog_stores s ON s.id = g.store_id "
                        + "WHERE g.customer_id = ? AND g.active IS TRUE "
                        + "ORDER BY g.created_at DESC, g.geofence_id DESC", GEOFENCE_MAPPER, customerId);
    }

    private CustomerGeofenceResponse addGeofenceForCustomer(
            Integer customerId, CreateCustomerGeofenceRequest request) {
        CustomerCatalogStoreResponse store = getStore(request.storeId());
        return jdbc.queryForObject("INSERT INTO mottainai.customer_geofence "
                        + "(customer_id, store_id, radius_meters, active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                        + "ON CONFLICT (customer_id, store_id) DO UPDATE SET radius_meters = EXCLUDED.radius_meters, "
                        + "active = TRUE, updated_at = CURRENT_TIMESTAMP "
                        + "RETURNING geofence_id, customer_id, store_id, ? AS store_name, radius_meters, active, created_at",
                GEOFENCE_MAPPER, customerId, request.storeId(), request.radiusMeters(), store.name());
    }

    private void removeGeofenceForCustomer(Integer customerId, Integer geofenceId) {
        int changed = jdbc.update("UPDATE mottainai.customer_geofence SET active = FALSE, "
                        + "updated_at = CURRENT_TIMESTAMP WHERE customer_id = ? AND geofence_id = ? AND active IS TRUE",
                customerId, geofenceId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Geofence was not found");
    }

    private Integer bootstrapFirebase(String firebaseUid) {
        Integer id = jdbc.queryForObject("SELECT mottainai.fn_bootstrap_customer_context(?)", Integer.class,
                ProtectedDataToken.externalAuthUid(firebaseUid));
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Firebase customer profile was not found");
        return id;
    }

    private Integer bootstrapLegacy(String protectedEmail) {
        Integer id = jdbc.queryForObject("SELECT mottainai.fn_api_bootstrap_legacy_customer_context(?)",
                Integer.class, protectedEmail);
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile was not found");
        return id;
    }

    private void requireLegacyOwner(Integer customerId, String protectedEmail) {
        Integer actual = bootstrapLegacy(protectedEmail);
        if (!actual.equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile was not found");
        }
    }

    private void requireFirebaseOwner(Integer customerId, String firebaseUid) {
        Integer actual = bootstrapFirebase(firebaseUid);
        if (!actual.equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile was not found");
        }
    }

    private CustomerResponse selectCustomer(Integer id) {
        return queryOne("SELECT " + CUSTOMER_COLUMNS + " FROM mottainai.customer WHERE customer_id = ? "
                + "AND active IS TRUE AND deleted_at IS NULL", CUSTOMER_MAPPER, id, "Customer profile was not found");
    }

    private PrivateEnvelope readEnvelope(Integer id) {
        return queryOne("SELECT payload_ciphertext, payload_nonce, key_version FROM mottainai.customer_private_data "
                        + "WHERE customer_id = ? AND deleted_at IS NULL",
                (rs, row) -> new PrivateEnvelope(rs.getBytes(1), rs.getBytes(2), rs.getShort(3)), id,
                "Customer private data was not found");
    }

    private FirebaseCustomerProfileResponse firebaseProfile(CustomerResponse base, String firebaseEmail) {
        PrivateEnvelope stored = readEnvelope(base.id());
        JsonNode payload;
        try {
            payload = objectMapper.readTree(cryptography.decrypt(base.id(),
                    new CustomerDataCryptography.Envelope(stored.ciphertext, stored.nonce, stored.keyVersion)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored customer personal data is not valid JSON", exception);
        }
        JsonNode addressNode = payload == null ? null : payload.get("address");
        AddressResponse address = addressNode == null || addressNode.isNull()
                ? null : objectMapper.convertValue(addressNode, AddressResponse.class);
        String email = firebaseEmail == null || firebaseEmail.isBlank()
                ? textValue(payload, "email") : firebaseEmail;
        String birthDateValue = textValue(payload, "birthDate");
        LocalDate birthDate = birthDateValue == null ? null : LocalDate.parse(birthDateValue);
        return new FirebaseCustomerProfileResponse(base.id(), base.birthYear(), base.active(), base.marketingConsent(),
                base.createdAt(), base.updatedAt(), textValue(payload, "fullName"), textValue(payload, "phone"),
                email, birthDate, address, textValue(payload, "avatarUrl"));
    }

    private static String textValue(JsonNode node, String field) {
        return node != null && node.path(field).isTextual() ? node.path(field).asText() : null;
    }

    private static void validateCloudinaryUrl(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    || uri.getPath() == null || uri.getPath().isBlank()
                    || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("URL must use the secure Cloudinary image host");
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "avatarUrl must be a valid HTTPS URL from res.cloudinary.com");
        }
    }

    private String profilePayload(String fullName, String cpf, String email, String phone, LocalDate birthDate,
            CreateAddressRequest address, String avatarUrl) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("fullName", fullName); value.put("cpf", cpf); value.put("email", email);
        if (phone == null) value.putNull("phone"); else value.put("phone", phone);
        if (birthDate == null) value.putNull("birthDate"); else value.put("birthDate", birthDate.toString());
        if (address == null) value.putNull("address"); else value.set("address", objectMapper.valueToTree(address));
        if (avatarUrl != null && !avatarUrl.isBlank()) value.put("avatarUrl", avatarUrl.trim());
        return writeJson(value);
    }

    private String addressToken(CreateAddressRequest address) {
        if (address == null) return null;
        return ProtectedDataToken.address(address.zipCode(), address.street(), address.number(), address.complement(),
                address.neighborhood(), address.city(), address.state());
    }

    private String writeJson(JsonNode value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Could not serialize customer personal data", exception); }
    }

    private CustomerResponse profileResult(ProfileResult result, String... successCodes) {
        for (String success : successCodes) if (success.equals(result.code)) return result.customer;
        if ("PROFILE_CONFLICT".equals(result.code) || "INACTIVE_IDENTITY_CONFLICT".equals(result.code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A customer profile already exists for conflicting identity data");
        }
        if ("NOT_FOUND".equals(result.code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile was not found");
        }
        throw new IllegalStateException("Firebase profile operation returned an unexpected database result");
    }

    private long count(String sql, List<Object> args) { return jdbc.queryForObject(sql, Long.class, args.toArray()); }

    private <T> T queryOne(String sql, RowMapper<T> mapper, Object argument, String message) {
        try { return jdbc.queryForObject(sql, mapper, argument); }
        catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    private static <T> PageResponse<T> page(List<T> content, int number, int size, long total) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        boolean empty = content.isEmpty();
        PageResponse.SortMetadata sort = new PageResponse.SortMetadata(true, false, false);
        PageResponse.PageableMetadata pageable = new PageResponse.PageableMetadata(
                sort, (long) number * size, number, size, true, false);
        return new PageResponse<>(content, pageable, pages, total, number >= pages - 1, size, number,
                sort, number == 0, content.size(), empty);
    }

    private void sendRecoveryEmail(Integer customerAuthId, String recipient, String token, String secret) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || recoveryUrl.isBlank() || mailFrom.isBlank()) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipient);
        message.setSubject("Mottainai password recovery");
        message.setText("Use this one-time password recovery token: " + token + "\nRecovery page: " + recoveryUrl);
        try {
            sender.send(message);
        } catch (RuntimeException exception) {
            LOGGER.warn("Customer recovery email delivery failed for auth ID {}; error type {}",
                    customerAuthId, exception.getClass().getSimpleName());
            try {
                jdbc.queryForObject("SELECT mottainai.fn_api_clear_customer_recovery(?,?,?)", Boolean.class,
                        customerAuthId, secret, Timestamp.valueOf(LocalDateTime.now()));
            } catch (RuntimeException cleanupException) {
                LOGGER.error("Could not clear an undelivered recovery token for auth ID {}; error type {}",
                        customerAuthId, cleanupException.getClass().getSimpleName());
            }
        }
    }

    private static RecoveryToken parseRecoveryToken(String value) {
        if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token is invalid");
        String[] parts = value.split("\\.", 2);
        if (parts.length != 2 || !parts[0].matches("[1-9][0-9]{0,9}")
                || !parts[1].matches("[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token is invalid");
        }
        try { return new RecoveryToken(Integer.parseInt(parts[0]), parts[1]); }
        catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token is invalid");
        }
    }

    private static CustomerResponse customer(ResultSet rs) throws SQLException {
        Object birthYearValue = rs.getObject("birth_year");
        Short birthYear = birthYearValue == null ? null : ((Number) birthYearValue).shortValue();
        return new CustomerResponse(rs.getInt("customer_id"), birthYear,
                (Boolean) rs.getObject("active"), (Boolean) rs.getObject("marketing_consent"),
                localDateTime(rs, "created_at"), localDateTime(rs, "updated_at"));
    }

    private static CustomerCatalogStoreResponse store(ResultSet rs, int row) throws SQLException {
        AddressResponse address = new AddressResponse((Integer) rs.getObject("address_id"), rs.getString("zip_code"),
                rs.getString("street"), rs.getString("number"), rs.getString("complement"),
                rs.getString("neighborhood"), rs.getString("city"), rs.getString("state"));
        return new CustomerCatalogStoreResponse(rs.getInt("id"), rs.getString("name"), address,
                rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"));
    }

    private static CustomerGeofenceResponse geofence(ResultSet rs, int row) throws SQLException {
        return new CustomerGeofenceResponse(rs.getInt("geofence_id"), rs.getInt("customer_id"),
                rs.getInt("store_id"), rs.getString("store_name"), rs.getInt("radius_meters"),
                (Boolean) rs.getObject("active"), localDateTime(rs, "created_at"));
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private record ProfileResult(String code, CustomerResponse customer) { }
    private record PrivateEnvelope(byte[] ciphertext, byte[] nonce, short keyVersion) { }
    private record RecoveryToken(Integer customerAuthId, String secret) { }
    private record PromotionBase(Integer id, String name, String description, String type,
            LocalDateTime startsAt, LocalDateTime endsAt, CustomerCatalogStoreResponse store) { }
    private record PromotionSummary(Integer id, BigDecimal distanceKm, BigDecimal discountPercentage) { }
}
