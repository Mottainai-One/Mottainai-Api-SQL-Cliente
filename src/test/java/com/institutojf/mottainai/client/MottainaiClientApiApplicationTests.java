package com.institutojf.mottainai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.annotation.Annotation;
import java.util.Arrays;

import com.institutojf.mottainai.client.config.OpenApiConfig;
import com.institutojf.mottainai.client.controller.ClientLoyaltyController;
import com.institutojf.mottainai.client.controller.CustomerCatalogController;
import com.institutojf.mottainai.client.controller.CustomerProfileController;
import com.institutojf.mottainai.client.controller.CustomerAuthenticationController;
import com.institutojf.mottainai.client.controller.CustomerGeofenceController;
import com.institutojf.mottainai.client.controller.ClientGeofenceController;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class MottainaiClientApiApplicationTests {

    @Test
    void openApiConfigDefinesBothBearerSchemes() {
        OpenAPI openApi = new OpenApiConfig().mottainaiClientOpenApi();

        assertNotNull(openApi.getComponents().getSecuritySchemes().get("FirebaseBearer"));
        assertNotNull(openApi.getComponents().getSecuritySchemes().get("LegacyCustomerBearer"));
    }

    @Test
    void clientRoutesMatchDatabaseContract() {
        assertEquals("/api/v1/client/auth/profile", basePath(CustomerProfileController.class));
        assertTrue(hasMapping(CustomerProfileController.class, "profile", GetMapping.class));
        assertTrue(hasMapping(CustomerProfileController.class, "createProfile", PostMapping.class));
        assertTrue(hasMapping(CustomerProfileController.class, "updateProfile", PutMapping.class));
        assertEquals("/api/v1/customer-catalog", basePath(CustomerCatalogController.class));
        assertTrue(hasMapping(CustomerCatalogController.class, "stores", GetMapping.class));
        assertTrue(hasMapping(CustomerCatalogController.class, "store", GetMapping.class));
        assertTrue(hasMapping(CustomerCatalogController.class, "promotions", GetMapping.class));
        assertTrue(hasMapping(CustomerCatalogController.class, "promotion", GetMapping.class));
        assertEquals("/api/v1/client/loyalty", basePath(ClientLoyaltyController.class));
        assertTrue(hasMapping(ClientLoyaltyController.class, "balance", GetMapping.class));
        assertTrue(hasMapping(ClientLoyaltyController.class, "transactions", GetMapping.class));
        assertEquals("/api/v1/customers/auth", basePath(CustomerAuthenticationController.class));
        assertTrue(hasMapping(CustomerAuthenticationController.class, "login", PostMapping.class));
        assertTrue(hasMapping(CustomerAuthenticationController.class, "requestPasswordRecovery", PostMapping.class));
        assertTrue(hasMapping(CustomerAuthenticationController.class, "validatePasswordReset", PostMapping.class));
        assertTrue(hasMapping(CustomerAuthenticationController.class, "resetPassword", PostMapping.class));
        assertTrue(hasMapping(CustomerAuthenticationController.class, "profile", GetMapping.class));
        assertEquals("/api/v1/customers/{customerId}/geofences", basePath(CustomerGeofenceController.class));
        assertTrue(hasMapping(CustomerGeofenceController.class, "list", GetMapping.class));
        assertTrue(hasMapping(CustomerGeofenceController.class, "add", PostMapping.class));
        assertTrue(hasMapping(CustomerGeofenceController.class, "remove", DeleteMapping.class));
        assertEquals("/api/v1/client/geofences", basePath(ClientGeofenceController.class));
        assertTrue(hasMapping(ClientGeofenceController.class, "list", GetMapping.class));
        assertTrue(hasMapping(ClientGeofenceController.class, "add", PostMapping.class));
        assertTrue(hasMapping(ClientGeofenceController.class, "remove", DeleteMapping.class));
        assertTrue(Arrays.stream(ClientLoyaltyController.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().equals("rewards") || method.getName().equals("redeem")));
    }

    private static String basePath(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        return mapping == null || mapping.value().length == 0 ? "" : mapping.value()[0];
    }

    private static <A extends Annotation> boolean hasMapping(
            Class<?> controller, String methodName, Class<A> mappingType) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .anyMatch(method -> method.isAnnotationPresent(mappingType));
    }

}

