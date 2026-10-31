package br.com.autoshop.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerSchemeName = "bearerAuth";
        final String basicSchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Autoshop API")
                        .version("1.0.0")
                        .description("Protected endpoints use a JWT bearer token. Use Basic Auth only to obtain a token at /auth."))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .components(new Components()
                        .addSecuritySchemes(basicSchemeName,
                                new SecurityScheme()
                                        .name(basicSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic"))
                        .addSecuritySchemes(bearerSchemeName,
                                new SecurityScheme()
                                        .name(bearerSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

}
