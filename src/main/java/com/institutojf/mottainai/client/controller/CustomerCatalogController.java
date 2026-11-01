package com.institutojf.mottainai.client.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.institutojf.mottainai.client.dto.request.CustomerPromotionCatalogQuery;
import com.institutojf.mottainai.client.dto.request.CustomerStoreCatalogQuery;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogPromotionResponse;
import com.institutojf.mottainai.client.dto.response.CustomerCatalogStoreResponse;
import com.institutojf.mottainai.client.dto.response.PageResponse;
import com.institutojf.mottainai.client.service.ClientApiOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer-catalog")
@SecurityRequirement(name = "FirebaseBearer")
@Tag(name = "Catálogo do cliente")
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
    @Operation(summary = "Listar promoções disponíveis",
            description = "latitude, longitude e radiusKm são opcionais, mas devem ser enviados juntos. "
                    + "A busca geográfica aceita raios de até 10 km e ordena por distância crescente, "
                    + "depois pelo maior desconto percentual da promoção.")
    public PageResponse<CustomerCatalogPromotionResponse> promotions(
            @Valid @ModelAttribute CustomerPromotionCatalogQuery query) {
        return operations.listPromotions(query);
    }

    @GetMapping("/promotions/{id}")
    public CustomerCatalogPromotionResponse promotion(@PathVariable Integer id) {
        return operations.getPromotion(id);
    }
}
