package com.clinica.mentalhealth.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro que añade headers de versionado a todas las respuestas de la API.
 *
 * Headers añadidos:
 * - X-API-Version: Versión actual de la API
 * - X-API-Deprecated: "true" si el endpoint está deprecado
 * - X-API-Sunset-Date: Fecha de sunset para endpoints deprecados
 *
 * Este filtro tiene la prioridad más baja para ejecutarse después de
 * todos los demás filtros y asegurar que los headers se añadan.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiVersionFilter implements WebFilter {

    private static final String CURRENT_VERSION = "1.0.0";
    private static final String DEPRECATED_PATH_PREFIX = "/api/";
    private static final String VERSIONED_PATH_PREFIX = "/api/v";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Solo procesar paths de la API
        if (!path.startsWith("/api")) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange)
            .doFinally(signalType -> {
                HttpHeaders headers = exchange.getResponse().getHeaders();

                // Siempre añadir versión actual
                headers.add(ApiVersion.Headers.API_VERSION, CURRENT_VERSION);

                // Detectar si es un path legacy (sin versión)
                if (isLegacyPath(path)) {
                    headers.add(ApiVersion.Headers.API_DEPRECATED, "true");
                    headers.add(ApiVersion.Headers.API_SUNSET_DATE, "2026-06-30");
                    headers.add(ApiVersion.Headers.API_SUCCESSOR, convertToVersionedPath(path));
                }
            });
    }

    /**
     * Verifica si el path es legacy (sin versión).
     * Un path legacy comienza con /api/ pero no con /api/v
     */
    private boolean isLegacyPath(String path) {
        return path.startsWith(DEPRECATED_PATH_PREFIX) &&
               !path.startsWith(VERSIONED_PATH_PREFIX) &&
               !path.startsWith("/api/actuator"); // Excluir actuator
    }

    /**
     * Convierte un path legacy al equivalente versionado.
     * Ejemplo: /api/patients -> /api/v1/patients
     */
    private String convertToVersionedPath(String legacyPath) {
        return legacyPath.replace("/api/", "/api/v1/");
    }
}
