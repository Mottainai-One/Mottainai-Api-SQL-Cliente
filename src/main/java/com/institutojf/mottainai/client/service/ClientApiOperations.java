package com.institutojf.mottainai.client.service;

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
import com.institutojf.mottainai.client.dto.response.CustomerResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyAccountResponse;
import com.institutojf.mottainai.client.dto.response.LoyaltyTransactionResponse;
import com.institutojf.mottainai.client.dto.response.PageResponse;
import com.institutojf.mottainai.client.dto.response.CustomerTokenResponse;
import com.institutojf.mottainai.client.dto.response.TokenValidationResponse;

public interface ClientApiOperations {

    CustomerResponse getFirebaseProfile(String firebaseUid);

    CustomerResponse createFirebaseProfile(
            CreateFirebaseCustomerProfileRequest request, String firebaseUid, String email);

    CustomerResponse updateFirebaseProfile(UpdateFirebaseCustomerProfileRequest request, String firebaseUid);

    PageResponse<CustomerCatalogStoreResponse> listStores(CustomerStoreCatalogQuery query);

    CustomerCatalogStoreResponse getStore(Integer storeId);

    PageResponse<CustomerCatalogPromotionResponse> listPromotions(CustomerPromotionCatalogQuery query);

    CustomerCatalogPromotionResponse getPromotion(Integer promotionId);

    LoyaltyAccountResponse getLoyaltyBalance(String firebaseUid);

    PageResponse<LoyaltyTransactionResponse> getLoyaltyTransactions(String firebaseUid, int page, int size);

    CustomerTokenResponse login(CustomerLoginRequest request);

    void requestPasswordRecovery(CustomerPasswordRecoveryRequest request);

    TokenValidationResponse validatePasswordReset(CustomerResetTokenRequest request);

    void resetPassword(CustomerPasswordResetRequest request);

    CustomerResponse getLegacyProfile(String protectedLoginEmail);

    List<CustomerGeofenceResponse> listGeofences(Integer customerId, String protectedLoginEmail);

    CustomerGeofenceResponse addGeofence(
            Integer customerId, CreateCustomerGeofenceRequest request, String protectedLoginEmail);

    void removeGeofence(Integer customerId, Integer geofenceId, String protectedLoginEmail);
}
