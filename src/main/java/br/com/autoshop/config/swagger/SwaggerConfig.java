package br.com.autoshop.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SwaggerConfig {

    private static final String SWAGGER_INITIALIZER = "swagger-initializer.js";
    private static final String SWAGGER_BUNDLE_MARKER = "window.ui = SwaggerUIBundle({";
    private static final String BASIC_AUTH_PLUGIN = """
            const BasicAuthLifecyclePlugin = () => ({
              statePlugins: {
                auth: {
                  wrapActions: {
                    authorizeWithPersistOption: (originalAction) => (authData) => {
                      const result = originalAction(authData);
                      const basic = authData && authData.basicAuth && authData.basicAuth.value;
                      if (basic && basic.username && typeof basic.password === 'string') {
                        const path = window.location.pathname;
                        const swaggerUiIndex = path.indexOf('/swagger-ui/');
                        const contextPath = swaggerUiIndex >= 0 ? path.slice(0, swaggerUiIndex) : '';
                        const authUrl = `${window.location.origin}${contextPath}/auth`;
                        const credentials = `${basic.username}:${basic.password}`;
                        const encodedCredentials = btoa(Array.from(
                          new TextEncoder().encode(credentials),
                          byte => String.fromCharCode(byte)
                        ).join(''));

                        fetch(authUrl, {
                          method: 'POST',
                          headers: { Authorization: `Basic ${encodedCredentials}` },
                          credentials: 'same-origin'
                        })
                          .then(response => {
                            if (!response.ok) throw new Error(`Authentication failed (${response.status})`);
                            return response.json();
                          })
                          .then(body => {
                            if (body && typeof body.token === 'string' && window.ui) {
                              window.ui.preauthorizeApiKey('bearerAuth', body.token);
                            }
                          })
                          .catch(error => console.error('Swagger Basic authentication failed', error));
                      }
                      return result;
                    },
                    logoutWithPersistOption: (originalAction) => (authNames) => {
                      const names = Array.isArray(authNames) ? [...authNames] : [authNames];
                      if (names.includes('basicAuth')) names.push('bearerAuth');
                      return originalAction([...new Set(names)]);
                    }
                  }
                }
              }
            });
            """;
    private static final String TOKEN_RESPONSE_INTERCEPTOR = """
              responseInterceptor: (response) => {
                const responsePath = new URL(response.url, window.location.href).pathname;
                if (response.status === 200 && responsePath.endsWith('/auth')) {
                  let body = response.body ?? response.obj ?? response.data;
                  if (typeof body === 'string') {
                    try { body = JSON.parse(body); } catch (_) { body = null; }
                  }
                  if (body && typeof body.token === 'string' && window.ui) {
                    window.ui.preauthorizeApiKey('bearerAuth', body.token);
                  }
                }
                return response;
              },
            """;

    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider
    ) {
        return new SwaggerIndexPageTransformer(
                swaggerUiConfig,
                swaggerUiOAuthProperties,
                swaggerWelcomeCommon,
                objectMapperProvider
        ) {
            @Override
            public Resource transform(
                    HttpServletRequest request,
                    Resource resource,
                    ResourceTransformerChain transformerChain
            ) throws IOException {
                Resource transformed = super.transform(request, resource, transformerChain);
                if (!SWAGGER_INITIALIZER.equals(resource.getFilename())) {
                    return transformed;
                }

                String initializer = new String(transformed.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String customizedInitializer = initializer
                        .replace(SWAGGER_BUNDLE_MARKER, BASIC_AUTH_PLUGIN + "\n" + SWAGGER_BUNDLE_MARKER)
                        .replace("plugins: [", "plugins: [\n      BasicAuthLifecyclePlugin,")
                        .replace(
                                SWAGGER_BUNDLE_MARKER,
                                SWAGGER_BUNDLE_MARKER + "\n" + TOKEN_RESPONSE_INTERCEPTOR
                        );
                return new TransformedResource(transformed, customizedInitializer.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

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
