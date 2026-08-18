package pe.smartcash.cash.shared.infrastructure.documentation.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Botón "Authorize" en Swagger UI: pega el token que devuelve {@code POST /api/v1/iam/sign-in}
 * (sin el prefijo {@code Bearer}) y queda adjunto a todos los intentos de request desde ahí.
 * El {@code bearerFormat} documenta HMAC-SHA256, no JWT: el token de este proyecto es un
 * esquema propio ({@code base64url(userId|expiresAt).base64url(firma)}, ver
 * {@code HmacTokenServiceAdapter}), no un JWT estándar de 3 segmentos.
 */
@Configuration
class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("SmartCash API")
                .version("v1.0.0")
                .description("API REST del backend de SmartCash, un MVP de finanzas personales."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("HMAC-SHA256")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
