# Mental Health Clinic API

API REST reactiva para gestion de clinica de salud mental con asistente de IA integrado.

## Tabla de Contenidos

1. [Requisitos](#requisitos)
2. [Configuracion Inicial](#configuracion-inicial)
3. [Arranque de la Aplicacion](#arranque-de-la-aplicacion)
4. [Endpoints](#endpoints)
5. [Autenticacion](#autenticacion)
6. [Asistente de IA](#asistente-de-ia)
7. [Monitoreo](#monitoreo)
8. [Estructura del Proyecto](#estructura-del-proyecto)

---

## Requisitos

- Java 17 o superior
- Docker y Docker Compose
- API Key de DeepSeek (<https://platform.deepseek.com/>)

---

## Configuracion Inicial

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd mental-health
```text

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

# Seguridad (solo produccion)
JWT_SECRET=tu-secret-seguro-de-al-menos-64-caracteres
```

### 3. Dar permisos al script

```bash
chmod +x docker.sh
```

---

## Arranque de la Aplicacion

### Comandos disponibles

| Comando             | Descripcion                              |
| ------------------- | ---------------------------------------- |
| `./docker.sh dev`   | Desarrollo: App + PostgreSQL en Docker   |
| `./docker.sh local` | App local (Maven) + PostgreSQL en Docker |
| `./docker.sh prod`  | Produccion: Todo en Docker, optimizado   |
| `./docker.sh db`    | Solo base de datos PostgreSQL            |

### Desarrollo completo (Docker)

Inicia la aplicacion y la base de datos en contenedores:

```bash
./docker.sh dev
```

Servicios disponibles:

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>
- PostgreSQL: localhost:5432

### Desarrollo local (Hot Reload)

Para desarrollo con recarga automatica de cambios:

```bash
./docker.sh local
```

Esto inicia PostgreSQL en Docker y la aplicacion con Maven (permite hot reload).

### Ver logs

```bash
./docker.sh dev-logs    # Logs de desarrollo
./docker.sh db-logs     # Logs de PostgreSQL
```

### Detener servicios

```bash
./docker.sh dev-stop    # Detener desarrollo
./docker.sh prod-stop   # Detener produccion
```

### Otros comandos utiles

```bash
./docker.sh status      # Estado de contenedores
./docker.sh db-shell    # Consola PostgreSQL (psql)
./docker.sh build       # Reconstruir imagen Docker
./docker.sh clean       # Limpiar contenedores y volumenes
./docker.sh help        # Ver todos los comandos
```

---

## Endpoints

### Publicos (sin autenticacion)

| Metodo | Ruta                 | Descripcion               |
| ------ | -------------------- | ------------------------- |
| GET    | `/swagger-ui.html`   | Documentacion interactiva |
| GET    | `/actuator/health`   | Estado de salud           |
| POST   | `/api/auth/login`    | Autenticacion             |
| POST   | `/api/auth/register` | Registro de usuario       |

### Protegidos (requieren JWT)

| Metodo | Ruta                 | Descripcion        |
| ------ | -------------------- | ------------------ |
| GET    | `/api/patients`      | Listar pacientes   |
| POST   | `/api/patients`      | Crear paciente     |
| GET    | `/api/psychologists` | Listar psicologos  |
| POST   | `/api/appointments`  | Crear cita         |
| GET    | `/api/rooms`         | Listar salas       |
| POST   | `/api/agent/chat`    | Interactuar con IA |

---

## Autenticacion

### Usuarios de prueba

| Usuario | Password | Rol               |
| ------- | -------- | ----------------- |
| admin   | 123      | ROLE_ADMIN        |
| doc     | 123      | ROLE_PSYCHOLOGIST |
| pepe    | 123      | ROLE_PATIENT      |

### Obtener token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}'
```

Respuesta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Usar token en peticiones

```bash
curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer <tu-token>"
```

### Usar token en Swagger UI

1. Abrir <http://localhost:8080/swagger-ui.html>
2. Click en el boton "Authorize"
3. Ingresar: `Bearer <tu-token>`
4. Click en "Authorize"
5. Ya puedes probar los endpoints protegidos

---

## Asistente de IA

El sistema incluye un asistente de IA que puede:

- Consultar informacion de pacientes
- Verificar disponibilidad de citas
- Sugerir horarios disponibles
- Responder preguntas en lenguaje natural

### Ejemplo de uso

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Authorization: Bearer <tu-token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"Que pacientes hay registrados?"}'
```

---

## Monitoreo

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metricas

```bash
curl http://localhost:8080/actuator/metrics
```

### Estadisticas de cache

```bash
curl http://localhost:8080/actuator/caches
```

---

## Estructura del Proyecto

```text
mental-health/
├── docker.sh                 # Script principal de comandos
├── docker-compose.yml        # Configuracion Docker
├── Dockerfile                # Imagen de la aplicacion
├── pom.xml                   # Dependencias Maven
├── .env.example              # Plantilla de variables de entorno
├── docker/
│   └── init-db/              # Scripts de inicializacion PostgreSQL
└── src/
    └── main/
        ├── java/com/clinica/mentalhealth/
        │   ├── ai/           # Configuracion de IA y Tools
        │   ├── config/       # Seguridad, OpenAPI, Cache
        │   ├── domain/       # Entidades JPA
        │   ├── repository/   # Repositorios R2DBC
        │   ├── security/     # JWT, Filtros
        │   ├── service/      # Logica de negocio
        │   └── web/          # Controladores REST
        └── resources/
            ├── application.properties      # Configuracion base
            ├── application-dev.properties  # Perfil desarrollo
            ├── application-prod.properties # Perfil produccion
            └── schema.sql                  # Esquema de base de datos
```

---

## Perfiles de Ejecucion

| Perfil | Base de Datos | Logs  | Swagger | Uso        |
| ------ | ------------- | ----- | ------- | ---------- |
| dev    | PostgreSQL    | DEBUG | Si      | Desarrollo |
| prod   | PostgreSQL    | WARN  | No      | Produccion |

Activar un perfil:

```bash
export SPRING_PROFILES_ACTIVE=dev
```

O en Docker Compose (ya configurado automaticamente).

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

## Solucion de Problemas

### La aplicacion no inicia

1. Verificar que Docker este corriendo: `docker info`
2. Verificar que PostgreSQL este saludable: `./docker.sh status`
3. Revisar logs: `./docker.sh dev-logs`

### Error de conexion a base de datos

1. Verificar que el contenedor de PostgreSQL este corriendo
2. Esperar unos segundos a que PostgreSQL este listo
3. Verificar credenciales en `.env`

### API Key de DeepSeek no funciona

1. Verificar que la key este configurada en `.env`
2. Verificar que la key sea valida en <https://platform.deepseek.com/>
3. Reiniciar la aplicacion despues de cambiar `.env`

---

## Licencia

Proyecto con fines educativos.
