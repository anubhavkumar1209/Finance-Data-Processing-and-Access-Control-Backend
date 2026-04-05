package com.financedashboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void openApiDeclaresJwtBearerSecurityScheme() {
        OpenAPI openAPI = openApiConfig.openAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Finance Dashboard API");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("bearerAuth");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getType())
                .isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getScheme())
                .isEqualTo("bearer");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat())
                .isEqualTo("JWT");
    }
}
