package com.libris.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the bearer scheme so Swagger UI has an "Authorize" button: without it the
 * documentation is readable but nothing on it can actually be tried out.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI librisOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Libris API")
                        .version("1.0.0")
                        .description("""
                                Sistema de préstamos para una biblioteca.

                                **Cómo probar la API desde aquí**
                                1. Ejecuta `POST /api/auth/login` con una de las cuentas de prueba
                                   documentadas en el README, por ejemplo `admin@libris.cl` / `Admin123!`.
                                2. Copia el valor de `token` de la respuesta.
                                3. Pulsa **Authorize** arriba a la derecha y pégalo.

                                Todos los errores se devuelven con la misma forma:
                                `{ timestamp, status, code, message, path, fieldErrors? }`.
                                El campo `code` es estable y está pensado para que el cliente
                                reaccione a una regla concreta sin leer el mensaje.
                                """)
                        .contact(new Contact().name("Equipo Libris").email("biblioteca@libris.local"))
                        .license(new License().name("Uso exclusivo del proceso de selección")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Entorno local")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token emitido por /api/auth/login")));
    }
}
