package com.charitymanagement.api.charitymanagementback.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI charityOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Charity Inventory Management System API")
                        .version("1.0.0")
                        .description("""
                                Backend API for charities, NGOs and donation centres: inventory, donations,
                                beneficiaries, aid distribution, volunteers, reporting and audit history.

                                **Authenticating from Swagger UI**
                                1. Call `POST /api/v1/auth/login` with an existing account.
                                2. Copy the `token` value from the response.
                                3. Press *Authorize* and paste the raw token (no `Bearer ` prefix).
                                """)
                        .contact(new Contact().name("Charity Inventory Management System")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT issued by /api/v1/auth/login")));
    }
}
