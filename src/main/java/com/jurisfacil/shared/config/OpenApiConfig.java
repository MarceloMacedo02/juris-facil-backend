package com.jurisfacil.shared.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@Profile({ "dev", "test" })
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI jfOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Juris-Fácil API")
                        .version("1.0.0-P0")
                        .description("API do MVP P0"))
                .servers(List.of(new Server().url("http://localhost:8080")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi workspaceApi() {
        return GroupedOpenApi.builder()
                .group("workspace")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomizer(openAPI -> addBearerRequirement(openAPI, "/api/v1/"))
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/v1/**")
                .addOpenApiCustomizer(openAPI -> addBearerRequirement(openAPI, "/api/admin/v1/"))
                .build();
    }

    private void addBearerRequirement(OpenAPI openAPI, String pathPrefix) {
        if (openAPI.getPaths() == null) {
            return;
        }
        openAPI.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith(pathPrefix)) {
                pathItem.readOperations().forEach(operation -> operation.addSecurityItem(
                        new SecurityRequirement().addList(BEARER_AUTH)));
            }
        });
    }
}
