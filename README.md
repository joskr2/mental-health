# Clinic Admin API / API Clínica Administrativa

Reactive REST API for mental health clinic management with AI assistant integration.  
API REST reactiva para gestión de clínica de salud mental con asistente IA integrado.

## Features / Características

- **Reactive API** / API Reactiva - Spring WebFlux + R2DBC
- **JWT Auth** - Access (30min) + Refresh tokens (14d) with theft detection / con detección de robo
- **AI Assistant** / Asistente IA - DeepSeek for natural language operations / operaciones en lenguaje natural
- **Rate Limiting** - API abuse protection / protección contra abuso
- **DB Migrations** / Migraciones - Flyway version control / control de versiones
- **Structured Logging** / Logs Estructurados - JSON for production / JSON en producción
- **Versioned API** / API Versionada - `/api/v1/` support / soporte

---

## Quick Start / Inicio Rápido

### Prerequisites / Requisitos

- Java 17+
- Docker + Docker Compose
- DeepSeek API Key: <https://platform.deepseek.com>

### Setup / Configuración

```bash
# Clone / Clonar
git clone <repo-url>
cd clinic-admin-api

# Configure / Configurar
cp .env.example .env
# Edit .env with your credentials / Editar .env con tus credenciales

# Start / Iniciar
chmod +x docker.sh
./docker.sh dev
```

**Services / Servicios:**

- API: <http://localhost:8080>
- Docs: <http://localhost:8080/docs>
- Health: <http://localhost:8080/actuator/health>
- PostgreSQL: localhost:5432

---

## Environment Variables / Variables de Entorno

```bash
# Database / Base de datos
POSTGRES_DB=mental_clinic
POSTGRES_USER=clinic_user
POSTGRES_PASSWORD=clinic_secret_2024

# AI (Required / Requerido)
DEEPSEEK_API_KEY=sk-your-key-here

# JWT Security (Min 32 chars / Mín 32 caracteres)
JWT_ACCESS_SECRET=your-access-secret-min-32-chars
JWT_REFRESH_SECRET=your-refresh-secret-min-32-chars
```

---

## Docker Commands / Comandos Docker

```bash
./docker.sh dev         # Development mode / Modo desarrollo
./docker.sh local       # Local dev (hot reload) / Dev local (recarga)
./docker.sh prod        # Production mode / Modo producción
./docker.sh db          # Database only / Solo base de datos
./docker.sh status      # Container status / Estado contenedores
./docker.sh dev-logs    # View logs / Ver logs
./docker.sh clean       # Clean all / Limpiar todo
./docker.sh help        # All commands / Todos los comandos
```

---

## Authentication / Autenticación

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}'
```

### Test Users / Usuarios de Prueba

| Username       | Password | Role              |
|----------------|----------|-------------------|
| admin          | 123      | ROLE_ADMIN        |
| doc            | 123      | ROLE_PSYCHOLOGIST |
| <pepe@test.com>  | 123      | ROLE_PATIENT      |

### Token Usage / Uso de Token

```bash
curl http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer <your-token>"
```

---

## AI Assistant / Asistente IA

**Administrative tasks only** / **Solo tareas administrativas**

### Can Do / Puede Hacer

- Book/cancel appointments / Agendar/cancelar citas
- Register/search patients / Registrar/buscar pacientes
- Check availability / Consultar disponibilidad
- Manage calendar / Gestionar calendario

### Cannot Do / NO Puede Hacer

- Clinical consultations / Consultas clínicas
- Diagnoses / Diagnósticos
- Therapy recommendations / Recomendaciones terapéuticas
- Medical advice / Asesoramiento médico

### Usage Example / Ejemplo de Uso

```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"text":"Schedule appointment for Juan Perez tomorrow at 3pm"}'
```

**Tools by Role / Herramientas por Rol:**

| Tool                    | Admin  | Psychologist | Patient |
|-------------------------|--------|--------------|---------|
| List appointments       | Yes/Si |Yes/Si        |Yes/Si.  |
| Book appointment        | Yes/Si |Yes/Si        | No      |
| Search patients         | Yes/Si |Yes/Si        | No      |
| Create psychologist     | Yes/Si |Yes/Si        | No      |

---

## Rate Limiting

| Endpoint      | Limit           | Purpose / Propósito              |
|---------------|-----------------|----------------------------------|
| Auth          | 10 req/min      | Brute force prevention / Anti fuerza bruta |
| AI/Chat       | 20 req/min      | API abuse prevention / Anti abuso API |
| General       | 100 req/min     | Normal usage / Uso normal |

**Response Headers:**

- `X-RateLimit-Limit`: Limit / Límite
- `X-RateLimit-Remaining`: Remaining / Restantes
- `Retry-After`: Seconds to reset / Segundos para reset

---

## API Endpoints

### Public / Públicos

| Method | Path                   | Description                      |
|--------|------------------------|----------------------------------|
| POST   | `/api/v1/auth/login`   | Login / Iniciar sesión          |
| POST   | `/api/v1/auth/refresh` | Refresh token / Renovar token   |
| GET    | `/actuator/health`     | Health check                     |
| GET    | `/docs`                | API documentation / Documentación|

### Protected / Protegidos

| Method | Path                      | Description                    |
|--------|---------------------------|--------------------------------|
| GET    | `/api/v1/patients`        | List patients / Listar pacientes |
| POST   | `/api/v1/patients`        | Create patient / Crear paciente |
| GET    | `/api/v1/psychologists`   | List psychologists / Listar psicólogos |
| POST   | `/api/v1/appointments`    | Create appointment / Crear cita |
| POST   | `/api/v1/agent/chat`      | AI assistant / Asistente IA    |

---

## Database Migrations / Migraciones

**Location / Ubicación:** `src/main/resources/db/migration/`

```sql
-- V3__add_notes.sql
ALTER TABLE appointments ADD COLUMN notes TEXT;
```

**Commands / Comandos:**

```bash
./mvnw flyway:info      # Status / Estado
./mvnw flyway:migrate   # Apply / Aplicar
./mvnw flyway:repair    # Fix / Reparar
```

---

## Testing / Pruebas

```bash
# All tests / Todas las pruebas
./mvnw test

# Unit tests only / Solo unitarias
./mvnw test -Dtest=*ServiceTest

# Coverage report / Reporte de cobertura
./mvnw test jacoco:report
```

---

## Project Structure / Estructura

``` text
src/main/java/com/clinica/mentalhealth/
├── ai/              # AI tools / Herramientas IA
├── config/          # Configuration / Configuración
├── domain/          # Entities / Entidades
├── repository/      # R2DBC repositories / Repositorios
├── security/        # JWT security / Seguridad JWT
├── service/         # Business logic / Lógica de negocio
└── web/             # REST controllers / Controladores REST
```

---

## Troubleshooting / Solución de Problemas

| Problem / Problema                | Solution / Solución                          |
|-----------------------------------|----------------------------------------------|
| App won't start / No inicia       | `docker info` + `./docker.sh status`        |
| DB connection error / Error DB    | Wait 5s / Esperar 5s, check `.env`          |
| Invalid API key / Key inválida    | Check DeepSeek console / Verificar consola  |
| JWT secret too short / Secret corto | Min 32 chars in `.env` / Mín 32 chars      |
| Rate limit 429                    | Wait 60s / Esperar 60s                      |

---
