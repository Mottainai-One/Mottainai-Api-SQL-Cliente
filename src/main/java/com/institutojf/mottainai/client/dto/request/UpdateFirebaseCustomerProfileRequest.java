package com.institutojf.mottainai.client.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Required profile fields are updated; an omitted or blank phone preserves its current value. */
public final class UpdateFirebaseCustomerProfileRequest {

    @NotBlank
    @Size(max = 150)
    @Schema(description = "Customer full name", example = "Cliente de Teste", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Size(max = 20)
    @Schema(description = "Optional phone. Omit or send blank to keep the current phone", example = "+5511999990000")
    private String phone;

    @NotNull
    @Schema(description = "Marketing consent", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean marketingConsent;

    @Size(max = 2048)
    @Schema(description = "Secure URL of the customer's Cloudinary profile image. Omit to keep it; send blank to clear it.")
    private String avatarUrl;
    private boolean avatarUrlSpecified;

    public UpdateFirebaseCustomerProfileRequest() {
    }

    public String fullName() {
        return fullName;
    }

    public String phone() {
        return phone;
    }

    public Boolean marketingConsent() {
        return marketingConsent;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public boolean avatarUrlSpecified() {
        return avatarUrlSpecified;
    }

    @JsonSetter("fullName")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonSetter("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @JsonSetter("marketingConsent")
    public void setMarketingConsent(Boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
    }

    @JsonSetter("avatarUrl")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.avatarUrlSpecified = true;
    }
}
