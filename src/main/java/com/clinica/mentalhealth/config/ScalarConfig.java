package com.clinica.mentalhealth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Configuración para servir Scalar API Documentation.
 *
 * Scalar es una alternativa moderna a Swagger UI que ofrece:
 * - Interfaz más limpia y moderna
 * - Mejor rendimiento
 * - Soporte para temas oscuro/claro
 * - Mejor experiencia de usuario
 *
 * Accesible en: /docs o /scalar
 */
@Configuration
public class ScalarConfig {

  @Bean
  public RouterFunction<ServerResponse> scalarRoutes() {
    String scalarHtml = buildScalarHtml();

    return RouterFunctions.route()
      .GET("/docs", request ->
        ServerResponse.ok()
          .contentType(requireNonNull(MediaType.TEXT_HTML))
          .bodyValue(requireNonNull(scalarHtml))
      )
      .GET("/scalar", request ->
        ServerResponse.ok()
          .contentType(requireNonNull(MediaType.TEXT_HTML))
          .bodyValue(requireNonNull(scalarHtml))
      )
      .GET("/docs/", request ->
        ServerResponse.ok()
          .contentType(requireNonNull(MediaType.TEXT_HTML))
          .bodyValue(requireNonNull(scalarHtml))
      )
      .build();
  }

  @NonNull
  private static MediaType requireNonNull(MediaType type) {
    if (type == null) throw new IllegalStateException("MediaType cannot be null");
    return type;
  }

  @NonNull
  private static String requireNonNull(String value) {
    if (value == null) throw new IllegalStateException("String cannot be null");
    return value;
  }

  private String buildScalarHtml() {
    return """
    <!DOCTYPE html>
    <html lang="es">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Mental Health Clinic API - Documentation</title>
        <meta name="description" content="API Documentation for Mental Health Clinic">
        <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🧠</text></svg>">
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                background: #0f0f23;
                color: #e0e0e0;
                min-height: 100vh;
            }

            /* Loading overlay */
            #loading-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%%;
                height: 100%%;
                background: #0f0f23;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                z-index: 9999;
                transition: opacity 0.5s ease-out;
            }

            #loading-overlay.hidden {
                opacity: 0;
                pointer-events: none;
            }

            .loader {
                width: 60px;
                height: 60px;
                border: 4px solid #333;
                border-top-color: #6366f1;
                border-radius: 50%%;
                animation: spin 1s linear infinite;
            }

            @keyframes spin {
                to { transform: rotate(360deg); }
            }

            .loading-text {
                margin-top: 20px;
                font-size: 16px;
                color: #888;
            }

            .loading-subtext {
                margin-top: 8px;
                font-size: 13px;
                color: #555;
            }

            /* Error state */
            #error-container {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                width: 100%%;
                height: 100%%;
                background: #0f0f23;
                justify-content: center;
                align-items: center;
                z-index: 9998;
            }

            #error-container.visible {
                display: flex;
            }

            .error-box {
                background: #1a1a2e;
                border: 1px solid #e74c3c;
                border-radius: 12px;
                padding: 40px;
                max-width: 500px;
                text-align: center;
            }

            .error-icon {
                font-size: 48px;
                margin-bottom: 16px;
            }

            .error-title {
                color: #e74c3c;
                font-size: 20px;
                margin-bottom: 12px;
            }

            .error-message {
                color: #888;
                font-size: 14px;
                line-height: 1.6;
                margin-bottom: 24px;
            }

            .retry-btn {
                background: #6366f1;
                color: white;
                border: none;
                padding: 12px 24px;
                border-radius: 8px;
                font-size: 14px;
                cursor: pointer;
                transition: background 0.2s;
            }

            .retry-btn:hover {
                background: #5558e3;
            }

            .alt-links {
                margin-top: 20px;
                font-size: 12px;
                color: #666;
            }

            .alt-links a {
                color: #6366f1;
                text-decoration: none;
            }

            .alt-links a:hover {
                text-decoration: underline;
            }
        </style>
    </head>
    <body>
        <!-- Loading overlay -->
        <div id="loading-overlay">
            <div class="loader"></div>
            <div class="loading-text">Cargando documentación de la API...</div>
            <div class="loading-subtext">Mental Health Clinic API v1.0</div>
        </div>

        <!-- Error container -->
        <div id="error-container">
            <div class="error-box">
                <div class="error-icon">⚠️</div>
                <h2 class="error-title">Error al cargar la documentación</h2>
                <p class="error-message" id="error-message">
                    No se pudo obtener la especificación OpenAPI del servidor.
                    El servidor puede estar sobrecargado o reiniciándose.
                </p>
                <button class="retry-btn" onclick="location.reload()">
                    Reintentar
                </button>
                <div class="alt-links">
                    <p>También puedes intentar:</p>
                    <a href="/static/openapi.json" target="_blank">Ver JSON del spec</a> |
                    <a href="/actuator/health" target="_blank">Estado del servidor</a>
                </div>
            </div>
        </div>

        <!-- Scalar API Reference -->
        <script
            id="api-reference"
            data-url="/static/openapi.json"
            data-configuration='{
                "theme": "kepler",
                "layout": "modern",
                "defaultHttpClient": {
                    "targetKey": "shell",
                    "clientKey": "curl"
                },
                "authentication": {
                    "preferredSecurityScheme": "Bearer Authentication",
                    "http": {
                        "bearer": {
                            "token": ""
                        }
                    }
                },
                "hiddenClients": ["unirest"],
                "searchHotKey": "k",
                "metaData": {
                    "title": "Mental Health Clinic API",
                    "description": "API reactiva para gestión de citas en clínica de salud mental"
                },
                "hideModels": false,
                "hideDownloadButton": false,
                "darkMode": true,
                "forceDarkModeState": "dark",
                "showSidebar": true
            }'
        ></script>

        <script>
            // Timeout for loading
            const LOAD_TIMEOUT = 15000; // 15 seconds
            const API_CHECK_TIMEOUT = 10000; // 10 seconds

            let loadingTimeout;
            let apiCheckComplete = false;

            // First, verify the API spec is available
            async function checkApiSpec() {
                try {
                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), API_CHECK_TIMEOUT);

                    const response = await fetch('/static/openapi.json', {
                        method: 'GET',
                        signal: controller.signal,
                        headers: {
                            'Accept': 'application/json'
                        }
                    });

                    clearTimeout(timeoutId);

                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }

                    apiCheckComplete = true;
                    console.log('✅ OpenAPI spec disponible');
                    return true;
                } catch (error) {
                    console.error('❌ Error al verificar OpenAPI spec:', error);
                    showError(error.message || 'No se pudo conectar con el servidor');
                    return false;
                }
            }

            function showError(message) {
                document.getElementById('loading-overlay').classList.add('hidden');
                document.getElementById('error-container').classList.add('visible');
                if (message) {
                    document.getElementById('error-message').textContent = message;
                }
            }

            function hideLoading() {
                const overlay = document.getElementById('loading-overlay');
                if (overlay && !overlay.classList.contains('hidden')) {
                    overlay.classList.add('hidden');
                    clearTimeout(loadingTimeout);
                }
            }

            // Set a timeout to show error if Scalar doesn't load
            loadingTimeout = setTimeout(() => {
                if (!apiCheckComplete) {
                    showError('Tiempo de espera agotado. El servidor puede estar ocupado generando la documentación.');
                }
            }, LOAD_TIMEOUT);

            // Check API first, then load Scalar
            checkApiSpec().then(available => {
                if (available) {
                    // Load Scalar from CDN
                    const script = document.createElement('script');
                    script.src = 'https://cdn.jsdelivr.net/npm/@scalar/api-reference';
                    script.onload = () => {
                        console.log('✅ Scalar cargado correctamente');
                        // Give Scalar a moment to render
                        setTimeout(hideLoading, 500);
                    };
                    script.onerror = () => {
                        showError('No se pudo cargar la librería Scalar desde CDN. Verifica tu conexión a internet.');
                    };
                    document.body.appendChild(script);
                }
            });

            // Fallback: hide loading when Scalar renders content
            const observer = new MutationObserver((mutations) => {
                for (const mutation of mutations) {
                    if (mutation.addedNodes.length > 0) {
                        const hasScalarContent = document.querySelector('[class*="scalar"]') ||
                                                 document.querySelector('[data-v-]');
                        if (hasScalarContent) {
                            hideLoading();
                            observer.disconnect();
                            break;
                        }
                    }
                }
            });

            observer.observe(document.body, { childList: true, subtree: true });
        </script>
    </body>
    </html>
    """;
  }
}
