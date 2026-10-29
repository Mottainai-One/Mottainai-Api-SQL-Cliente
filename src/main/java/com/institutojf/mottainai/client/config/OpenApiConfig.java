package com.institutojf.mottainai.client.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    ObjectMapper customerPayloadObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public OpenAPI mottainaiClientOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mottainai Client API")
                        .version("v1")
                        .description("REST API for the Mottainai customer mobile application. "
                                + "Protected endpoints require the bearer token issued by their configured identity provider.")
                        .contact(new Contact().name("Mottainai API Team")))
                .components(new Components()
                        .addSecuritySchemes("FirebaseBearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Firebase ID token for client profile, loyalty, geofence, and catalog endpoints."))
                        .addSecuritySchemes("LegacyCustomerBearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Customer access token returned by the legacy customer login endpoint.")));
    }
}
