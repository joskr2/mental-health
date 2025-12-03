# Mental Health Clinic API

API REST reactiva para gestión de clínica de salud mental con asistente de IA integrado.

## 🚀 Características

- **API Reactiva** - Spring WebFlux + R2DBC para máximo rendimiento
- **Autenticación JWT** - Access tokens (30 min) + Refresh tokens (14 días) con detección de robo
- **Asistente IA** - Integración con DeepSeek para operaciones clínicas en lenguaje natural
- **Rate Limiting** - Protección contra abuso de la API
- **Migraciones BD** - Flyway para control de versiones del esquema
- **Logging Estructurado** - JSON en producción para agregadores (ELK, CloudWatch)
- **API Versionada** - Soporte para `/api/v1/` con headers de deprecación

## Tabla de Contenidos

1. [Requisitos](#requisitos)
2. [Configuración Inicial](#configuración-inicial)
3. [Arranque de la Aplicación](#arranque-de-la-aplicación)
4. [Endpoints](#endpoints)
5. [Autenticación](#autenticación)
6. [Asistente de IA](#asistente-de-ia)
7. [Rate Limiting](#rate-limiting)
8. [Migraciones de Base de Datos](#migraciones-de-base-de-datos)
9. [Monitoreo](#monitoreo)
10. [Estructura del Proyecto](#estructura-del-proyecto)
11. [Testing](#testing)

---

## Requisitos

- Java 17 o superior
- Docker y Docker Compose
- API Key de DeepSeek (<https://platform.deepseek.com/>)

---

## Configuración Inicial

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd mental-health
```

### 2. Configurar variables de entorno

Copiar el archivo de ejemplo y editar con tus credenciales:

```bash
cp .env.example .env
```

Editar `.env` con los siguientes valores:

```properties
# Base de datos
POSTGRES_DB=mental_clinic
POSTGRES_USER=clinic_user
POSTGRES_PASSWORD=clinic_secret_2024

# API Key de IA (requerida)
DEEPSEEK_API_KEY=sk-tu-api-key-aqui

# Seguridad JWT (OBLIGATORIO en producción)
# Mínimo 32 caracteres cada uno
JWT_ACCESS_SECRET=tu-secret-de-access-token-muy-seguro-min-32-chars
JWT_REFRESH_SECRET=tu-secret-de-refresh-token-muy-seguro-min-32-chars
```

### 3. Dar permisos al script

```bash
chmod +x docker.sh
```

---

## Arranque de la Aplicación

### Comandos disponibles

| Comando             | Descripción                              |
| ------------------- | ---------------------------------------- |
| `./docker.sh dev`   | Desarrollo: App + PostgreSQL en Docker   |
| `./docker.sh local` | App local (Maven) + PostgreSQL en Docker |
| `./docker.sh prod`  | Producción: Todo en Docker, optimizado   |
| `./docker.sh db`    | Solo base de datos PostgreSQL            |

### Desarrollo completo (Docker)

Inicia la aplicación y la base de datos en contenedores:

```bash
./docker.sh dev
```

Servicios disponibles:

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>
- PostgreSQL: localhost:5432

### Desarrollo local (Hot Reload)

Para desarrollo con recarga automática de cambios:

```bash
./docker.sh local
```

Esto inicia PostgreSQL en Docker y la aplicación con Maven (permite hot reload).

### Ver logs

```bash
./docker.sh dev-logs    # Logs de desarrollo
./docker.sh db-logs     # Logs de PostgreSQL
```

### Detener servicios

```bash
./docker.sh dev-stop    # Detener desarrollo
./docker.sh prod-stop   # Detener producción
```

### Otros comandos útiles

```bash
./docker.sh status      # Estado de contenedores
./docker.sh db-shell    # Consola PostgreSQL (psql)
./docker.sh build       # Reconstruir imagen Docker
./docker.sh clean       # Limpiar contenedores y volúmenes
./docker.sh help        # Ver todos los comandos
```

---

## Endpoints

### API Versionada

Todos los endpoints ahora soportan versionado. Se recomienda usar `/api/v1/`:

| Versión | Prefijo     | Estado      |
| ------- | ----------- | ----------- |
| v1      | `/api/v1/`  | ✅ Activo   |
| Legacy  | `/api/`     | ⚠️ Deprecado |

Los endpoints legacy añaden headers de deprecación:
- `X-API-Deprecated: true`
- `X-API-Sunset-Date: 2026-06-30`
- `X-API-Successor: /api/v1/...`

### Públicos (sin autenticación)

| Método | Ruta                    | Descripción               |
| ------ | ----------------------- | ------------------------- |
| GET    | `/swagger-ui.html`      | Documentación interactiva |
| GET    | `/actuator/health`      | Estado de salud           |
| POST   | `/api/v1/auth/login`    | Autenticación             |
| POST   | `/api/v1/auth/refresh`  | Renovar tokens            |

### Protegidos (requieren JWT)

| Método | Ruta                    | Descripción        |
| ------ | ----------------------- | ------------------ |
| GET    | `/api/v1/patients`      | Listar pacientes   |
| POST   | `/api/v1/patients`      | Crear paciente     |
| GET    | `/api/v1/psychologists` | Listar psicólogos  |
| POST   | `/api/v1/appointments`  | Crear cita         |
| GET    | `/api/v1/rooms`         | Listar salas       |
| POST   | `/api/v1/agent/chat`    | Interactuar con IA |

---

## Autenticación

### Sistema de Tokens Duales

El sistema implementa tokens de acceso y refresh con seguridad máxima:

| Token         | Duración  | Propósito                    |
| ------------- | --------- | ---------------------------- |
| Access Token  | 30 min    | Autenticación de requests    |
| Refresh Token | 14 días   | Renovar access tokens        |

### Características de Seguridad

- **Tokens de un solo uso**: El refresh token se invalida al usarlo
- **Detección de robo**: Si se reutiliza un token revocado, se cierran TODAS las sesiones
- **Límite de sesiones**: Máximo 5 sesiones activas por usuario
- **Rotación automática**: Cada refresh genera un nuevo par de tokens

### Usuarios de prueba

| Usuario         | Password | Rol               |
| --------------- | -------- | ----------------- |
| admin           | 123      | ROLE_ADMIN        |
| doc             | 123      | ROLE_PSYCHOLOGIST |
| pepe@test.com   | 123      | ROLE_PATIENT      |

### Obtener token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}'
```

Respuesta:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Renovar tokens

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<tu-refresh-token>"}'
```

### Usar token en peticiones

```bash
curl -X GET http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer <tu-access-token>"
```

### Usar token en Swagger UI

1. Abrir <http://localhost:8080/swagger-ui.html>
2. Click en el botón "Authorize"
3. Ingresar: `Bearer <tu-access-token>`
4. Click en "Authorize"
5. Ya puedes probar los endpoints protegidos

---

## Asistente de IA

El sistema incluye un asistente de IA que puede:

- Consultar información de pacientes
- Verificar disponibilidad de citas
- Sugerir horarios disponibles
- Crear pacientes y citas
- Responder preguntas en lenguaje natural

### Herramientas por Rol

| Herramienta            | Admin | Psicólogo | Descripción                    |
| ---------------------- | ----- | --------- | ------------------------------ |
| calculateDateTool      | ✅    | ✅        | Calcular fechas relativas      |
| searchPatientTool      | ✅    | ✅        | Buscar pacientes               |
| createPatientTool      | ✅    | ✅        | Crear pacientes                |
| bookAppointmentTool    | ✅    | ✅        | Agendar citas                  |
| listRoomsTool          | ✅    | ✅        | Listar salas                   |
| createPsychologistTool | ✅    | ❌        | Crear psicólogos (solo Admin)  |
| createRoomTool         | ✅    | ❌        | Crear salas (solo Admin)       |

### Ejemplo de uso

```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Authorization: Bearer <tu-token>" \
  -H "Content-Type: application/json" \
  -d '{"text":"Agenda una cita para Pepe Grillo el próximo lunes a las 10am"}'
```

---

## Rate Limiting

La API implementa rate limiting para protección contra abuso:

| Tipo de Endpoint | Límite           | Descripción                    |
| ---------------- | ---------------- | ------------------------------ |
| Autenticación    | 10 req/minuto    | Prevenir fuerza bruta          |
| IA/Chat          | 20 req/minuto    | Prevenir abuso de API externa  |
| General          | 100 req/minuto   | Uso normal de la API           |

### Headers de respuesta

- `X-RateLimit-Limit`: Límite de requests por minuto
- `X-RateLimit-Remaining`: Requests restantes
- `Retry-After`: Segundos hasta reset (cuando límite excedido)

### Respuesta cuando se excede el límite

```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
Retry-After: 60
```

---

## Migraciones de Base de Datos

El proyecto usa **Flyway** para gestionar migraciones de base de datos.

### Ubicación de migraciones

```
src/main/resources/db/migration/
├── V1__initial_schema.sql    # Esquema inicial
└── V2__seed_data.sql         # Datos de prueba
```

### Convención de nombres

```
V{version}__{description}.sql
```

- `V1__initial_schema.sql` - Versión 1, esquema inicial
- `V2__seed_data.sql` - Versión 2, datos de prueba

### Crear nueva migración

```sql
-- V3__add_appointment_notes.sql
ALTER TABLE appointments ADD COLUMN notes TEXT;
```

### Comandos útiles

```bash
# Ver estado de migraciones
./mvnw flyway:info

# Aplicar migraciones pendientes
./mvnw flyway:migrate

# Reparar historial corrupto (solo desarrollo)
./mvnw flyway:repair
```

---

## Monitoreo

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Métricas

```bash
curl http://localhost:8080/actuator/metrics
```

### Estadísticas de caché

```bash
curl http://localhost:8080/actuator/caches
```

### Logging

**Desarrollo**: Logs legibles con colores en consola

**Producción**: Logs JSON estructurados para agregadores

```json
{
  "timestamp": "2025-01-15T10:30:00.000-05:00",
  "level": "INFO",
  "logger": "c.c.mentalhealth.service.AppointmentService",
  "message": "Cita creada con ID 123"
}
```

---

## Estructura del Proyecto

```
mental-health/
├── docker.sh                    # Script principal de comandos
├── docker-compose.yml           # Configuración Docker
├── Dockerfile                   # Imagen de la aplicación
├── pom.xml                      # Dependencias Maven
├── docker/
│   └── init-db/                 # Scripts de inicialización PostgreSQL
└── src/
    ├── main/
    │   ├── java/com/clinica/mentalhealth/
    │   │   ├── ai/              # Herramientas de IA
    │   │   │   └── tools/       # DTOs para function calling
    │   │   ├── config/          # Configuración (Security, Cache, Rate Limit, etc.)
    │   │   ├── domain/          # Entidades (User, Patient, Appointment, etc.)
    │   │   ├── repository/      # Repositorios R2DBC
    │   │   ├── security/        # JWT, Filtros de autenticación
    │   │   ├── service/         # Lógica de negocio
    │   │   └── web/             # Controladores REST
    │   │       ├── dto/         # DTOs de request/response
    │   │       └── exception/   # Manejo global de errores
    │   └── resources/
    │       ├── db/migration/    # Migraciones Flyway
    │       ├── application.properties       # Configuración base
    │       ├── application-dev.properties   # Perfil desarrollo
    │       ├── application-prod.properties  # Perfil producción
    │       └── logback-spring.xml           # Configuración de logging
    └── test/
        ├── java/                # Tests unitarios y de integración
        └── resources/           # Configuración para tests
```

---

## Testing

### Ejecutar todos los tests

```bash
./mvnw test
```

### Ejecutar tests específicos

```bash
# Tests unitarios (rápidos, sin Docker)
./mvnw test -Dtest=*ServiceTest

# Tests de integración (requieren Docker)
./mvnw test -Dtest=*IntegrationTest
```

### Cobertura de tests

```bash
./mvnw test jacoco:report
# Ver reporte en target/site/jacoco/index.html
```

### Tests disponibles

| Test                                  | Tipo        | Descripción                           |
| ------------------------------------- | ----------- | ------------------------------------- |
| `AppointmentServiceTest`              | Unitario    | Validaciones de citas                 |
| `PatientServiceTest`                  | Unitario    | CRUD de pacientes                     |
| `DateCalculationServiceTest`          | Unitario    | Cálculo de fechas relativas           |
| `JwtServiceTest`                      | Unitario    | Generación/validación de tokens       |
| `AppointmentRepositoryIntegrationTest`| Integración | Queries con PostgreSQL real           |

---

## Perfiles de Ejecución

| Perfil | Base de Datos | Logs        | Swagger | Rate Limit | Uso        |
| ------ | ------------- | ----------- | ------- | ---------- | ---------- |
| dev    | PostgreSQL    | DEBUG       | Sí      | Sí         | Desarrollo |
| prod   | PostgreSQL    | JSON/WARN   | No      | Sí         | Producción |
| test   | H2 / Testcontainers | WARN  | No      | No         | Testing    |

Activar un perfil:

```bash
export SPRING_PROFILES_ACTIVE=dev
```

O en Docker Compose (ya configurado automáticamente).

---

## Comandos Maven

```bash
# Compilar
./mvnw clean compile

# Ejecutar tests
./mvnw test

# Empaquetar (genera JAR)
./mvnw clean package -DskipTests

# Ejecutar directamente
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Solución de Problemas

### La aplicación no inicia

1. Verificar que Docker esté corriendo: `docker info`
2. Verificar que PostgreSQL esté saludable: `./docker.sh status`
3. Revisar logs: `./docker.sh dev-logs`

### Error de conexión a base de datos

1. Verificar que el contenedor de PostgreSQL esté corriendo
2. Esperar unos segundos a que PostgreSQL esté listo
3. Verificar credenciales en `.env`

### API Key de DeepSeek no funciona

1. Verificar que la key esté configurada en `.env`
2. Verificar que la key sea válida en <https://platform.deepseek.com/>
3. Reiniciar la aplicación después de cambiar `.env`

### Error "Secret key too short"

1. Las claves JWT deben tener al menos 32 caracteres
2. Verificar `JWT_ACCESS_SECRET` y `JWT_REFRESH_SECRET` en `.env`

### Rate limit excedido (429)

1. Esperar 60 segundos para que se resetee el límite
2. Verificar el header `Retry-After` en la respuesta

---

## Licencia

Proyecto con fines educativos.