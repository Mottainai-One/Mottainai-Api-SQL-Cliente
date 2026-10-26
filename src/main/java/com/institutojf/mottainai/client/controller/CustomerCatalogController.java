package com.institutojf.mottainai.client.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.institutojf.mottainai.client.dto.request.CustomerPromotionCatalogQuery;
import com.institutojf.mottainai.client.dto.request.CustomerStoreCatalogQuery;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogPromotionResponse;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogStoreResponse;
import com.institutojf.mottainai.client.dto.response.PageResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-catalog")
@SecurityRequirement(name = "FirebaseBearer")
public class CustomerCatalogController {

    private final ClientApiOperations operations;

    public CustomerCatalogController(ClientApiOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/stores")
    public PageResponse<CustomerCatalogStoreResponse> stores(
            @Valid @ModelAttribute CustomerStoreCatalogQuery query) {
        return operations.listStores(query);
    }

    @GetMapping("/stores/{id}")
    public CustomerCatalogStoreResponse store(
            @PathVariable Integer id) {
        return operations.getStore(id);
    }

    @GetMapping("/promotions")
    public PageResponse<CustomerCatalogPromotionResponse> promotions(
            @Valid @ModelAttribute CustomerPromotionCatalogQuery query) {
        return operations.listPromotions(query);
    }

    @GetMapping("/promotions/{id}")
    public CustomerCatalogPromotionResponse promotion(@PathVariable Integer id) {
        return operations.getPromotion(id);
    }
}
