package com.clinica.mentalhealth.config;

/**
 * Constantes para el versionado de la API.
 *
 * Estrategia de versionado: URL Path Versioning
 * Ejemplo: /api/v1/patients, /api/v2/patients
 *
 * Ventajas:
 * - Fácil de entender y usar
 * - Visible en la URL
 * - Cacheable por CDN
 * - Compatible con Swagger/OpenAPI
 *
 * Política de deprecación:
 * - Las versiones antiguas se mantienen por al menos 6 meses
 * - Se notifica deprecación con headers: X-API-Deprecated, X-API-Sunset-Date
 * - La documentación indica claramente versiones activas vs deprecated
 */
public final class ApiVersion {

    private ApiVersion() {
        // Utility class - prevent instantiation
    }

    // ==================== Current Versions ====================

    /**
     * Versión actual de la API (v1).
     * Usar esta constante para todos los nuevos endpoints.
     */
    public static final String V1 = "/api/v1";

    /**
     * Prefijo base de la API sin versión.
     * Solo usar para endpoints que no requieren versionado (health, metrics).
     */
    public static final String BASE = "/api";

    // ==================== Path Helpers ====================

    /**
     * Construye el path completo para un recurso en v1.
     * Ejemplo: path("patients") -> "/api/v1/patients"
     */
    public static String v1(String resource) {
        return V1 + "/" + resource;
    }

    // ==================== Resource Paths ====================

    /**
     * Paths de recursos para v1.
     * Centraliza todas las rutas para fácil mantenimiento.
     */
    public static final class Resources {
        private Resources() {}

        // Authentication
        public static final String AUTH = V1 + "/auth";
        public static final String AUTH_LOGIN = AUTH + "/login";
        public static final String AUTH_REFRESH = AUTH + "/refresh";
        public static final String AUTH_LOGOUT = AUTH + "/logout";
        public static final String AUTH_SESSIONS = AUTH + "/sessions";

        // Patients
        public static final String PATIENTS = V1 + "/patients";
        public static final String PATIENTS_SEARCH = PATIENTS + "/search";

        // Psychologists
        public static final String PSYCHOLOGISTS = V1 + "/psychologists";

        // Appointments
        public static final String APPOINTMENTS = V1 + "/appointments";

        // Rooms
        public static final String ROOMS = V1 + "/rooms";

        // AI Agent
        public static final String AGENT = V1 + "/agent";
        public static final String AGENT_CHAT = AGENT + "/chat";
    }

    // ==================== Headers ====================

    /**
     * Headers relacionados con versionado de API.
     */
    public static final class Headers {
        private Headers() {}

        /**
         * Header que indica la versión de la API usada en la respuesta.
         */
        public static final String API_VERSION = "X-API-Version";

        /**
         * Header que indica si el endpoint está deprecado.
         */
        public static final String API_DEPRECATED = "X-API-Deprecated";

        /**
         * Header que indica la fecha de sunset del endpoint deprecado.
         * Formato: ISO-8601 (2025-12-31)
         */
        public static final String API_SUNSET_DATE = "X-API-Sunset-Date";

        /**
         * Header que sugiere la nueva versión del endpoint.
         */
        public static final String API_SUCCESSOR = "X-API-Successor";
    }

    // ==================== Legacy Paths (for migration) ====================

    /**
     * Paths legacy sin versión (para mantener compatibilidad temporal).
     * Estos paths deben migrar a v1 eventualmente.
     *
     * @deprecated Usar paths versionados de {@link Resources}
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public static final class Legacy {
        private Legacy() {}

        @Deprecated(since = "1.0.0", forRemoval = true)
        public static final String AUTH = "/api/auth";
        @Deprecated(since = "1.0.0", forRemoval = true)
        public static final String PATIENTS = "/api/patients";
        @Deprecated(since = "1.0.0", forRemoval = true)
        public static final String PSYCHOLOGISTS = "/api/psychologists";
        @Deprecated(since = "1.0.0", forRemoval = true)
        public static final String APPOINTMENTS = "/api/appointments";
        @Deprecated(since = "1.0.0", forRemoval = true)
        public static final String ROOMS = "/api/rooms";
        @Deprecated(since = "1.0.0", forRemoval = true)
        public static final String AGENT = "/api/agent";
    }
}
