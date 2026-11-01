package com.institutojf.mottainai.client.service;

import java.time.LocalDate;
import java.util.List;

import com.institutojf.mottainai.client.dto.request.CreateCustomerGeofenceRequest;
import com.institutojf.mottainai.client.dto.request.CreateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.request.CustomerLoginRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordRecoveryRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPasswordResetRequest;
import com.institutojf.mottainai.client.dto.request.CustomerResetTokenRequest;
import com.institutojf.mottainai.client.dto.request.CustomerPromotionCatalogQuery;
import com.institutojf.mottainai.client.dto.request.CustomerStoreCatalogQuery;
import com.institutojf.mottainai.client.dto.request.UpdateFirebaseCustomerProfileRequest;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogPromotionResponse;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogStoreResponse;
import com.institutojf.mottainai.client.dto.response.CustomerGeofenceResponse;
import com.institutojf.mottainai.client.dto.response.CustomerLoyaltyRewardResponse;
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.dto.response.FirebaseCustomerProfileResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyTransactionResponse;
import com.institutojf.mottainai.client.dto.response.PageResponse;
import com.institutojf.mottainai.client.dto.response.CustomerTokenResponse;
import com.institutojf.mottainai.client.dto.response.TokenValidationResponse;

public interface ClientApiOperations {

    FirebaseCustomerProfileResponse getFirebaseProfile(String firebaseUid, String firebaseEmail);

    String getFirebaseProfileAvatarUrl(String firebaseUid);

    FirebaseCustomerProfileResponse createFirebaseProfile(
            CreateFirebaseCustomerProfileRequest request, String firebaseUid, String email);

    FirebaseCustomerProfileResponse updateFirebaseProfile(
            UpdateFirebaseCustomerProfileRequest request, String firebaseUid, String firebaseEmail);

    PageResponse<CustomerCatalogStoreResponse> listStores(CustomerStoreCatalogQuery query);

    CustomerCatalogStoreResponse getStore(Integer storeId);

    PageResponse<CustomerCatalogPromotionResponse> listPromotions(CustomerPromotionCatalogQuery query);

    CustomerCatalogPromotionResponse getPromotion(Integer promotionId);

    LoyaltyAccountResponse getLoyaltyBalance(String firebaseUid);

    List<CustomerLoyaltyRewardResponse> getLoyaltyRewards(String firebaseUid);

    PageResponse<LoyaltyTransactionResponse> getLoyaltyTransactions(
            String firebaseUid, int page, int size, LocalDate from, LocalDate to);

    CustomerTokenResponse login(CustomerLoginRequest request);

    void requestPasswordRecovery(CustomerPasswordRecoveryRequest request);

    TokenValidationResponse validatePasswordReset(CustomerResetTokenRequest request);

    void resetPassword(CustomerPasswordResetRequest request);

    CustomerResponse getLegacyProfile(String protectedLoginEmail);

    List<CustomerGeofenceResponse> listMyGeofences(String firebaseUid);

    CustomerGeofenceResponse addMyGeofence(CreateCustomerGeofenceRequest request, String firebaseUid);

    void removeMyGeofence(Integer geofenceId, String firebaseUid);

    List<CustomerGeofenceResponse> listGeofences(Integer customerId, String protectedLoginEmail);

    CustomerGeofenceResponse addGeofence(
            Integer customerId, CreateCustomerGeofenceRequest request, String protectedLoginEmail);

    void removeGeofence(Integer customerId, Integer geofenceId, String protectedLoginEmail);
}

