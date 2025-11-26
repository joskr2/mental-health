# 🏥 Mental Health Clinic API

API REST reactiva para gestión de clínica de salud mental con asistente de IA integrado.

## 🚀 Inicio Rápido

### Opción 1: Script automatizado
```bash
./start.sh
```

### Opción 2: Manual
```bash
# 1. Configurar API Key
export DEEPSEEK_API_KEY=tu_api_key_aqui

# 2. Iniciar aplicación
./mvnw spring-boot:run

# 3. Acceder a Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## 📋 Requisitos

- ✅ Java 17 o superior
- ✅ Maven 3.8+
- ✅ API Key de DeepSeek (https://platform.deepseek.com/)

---

## 🎯 Características

### 🔐 Seguridad
- Autenticación JWT
- Roles: ADMIN, PSYCHOLOGIST, PATIENT
- Autorización por método (@PreAuthorize)

### 💾 Base de Datos
- H2 en memoria (modo PostgreSQL)
- R2DBC (Reactive Database Connectivity)
- Inicialización automática de datos

### ⚡ Cache
- Caffeine Cache
- 500 entradas máximas
- Expiración: 30 minutos

### 🤖 IA Integrada
- DeepSeek Chat Model
- Asistente clínico con herramientas (tools)
- Consulta de pacientes y citas

### 📖 Documentación
- Swagger UI (acceso público)
- OpenAPI 3.0
- Endpoints interactivos

### 📊 Monitoreo
- Spring Boot Actuator
- Health checks
- Métricas de rendimiento
- Estadísticas de caché

---

## 🌐 Endpoints Principales

### 🔓 Públicos (sin autenticación)
- `GET /swagger-ui.html` - Documentación interactiva
- `GET /actuator/health` - Estado de salud
- `POST /api/auth/login` - Autenticación

### 🔐 Protegidos (requieren JWT)
- `GET /api/patients` - Listar pacientes
- `POST /api/patients` - Crear paciente
- `GET /api/psychologists` - Listar psicólogos
- `POST /api/appointments` - Crear cita
- `POST /api/agent/chat` - Interactuar con IA

---

## 👤 Usuarios de Prueba

| Username | Password | Rol |
|----------|----------|-----|
| admin | 123 | ROLE_ADMIN |
| doc | 123 | ROLE_PSYCHOLOGIST |
| pepe | 123 | ROLE_PATIENT |

---

## 🔑 Autenticación

### 1. Obtener Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}'
```

### 2. Usar Token
```bash
curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

### 3. En Swagger UI
1. Click en **"Authorize"** 🔒
2. Ingresa: `Bearer TU_TOKEN_AQUI`
3. Click "Authorize"
4. ✅ Listo para probar endpoints protegidos

---

## 🤖 Asistente de IA

### Características
- Consulta información de pacientes
- Verifica disponibilidad de citas
- Sugiere horarios disponibles
- Responde en lenguaje natural

### Ejemplo de Uso
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"¿Qué pacientes hay registrados?"}'
```

---

## 📊 Monitoreo

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Métricas
```bash
curl http://localhost:8080/actuator/metrics
```

### Estadísticas de Caché
```bash
curl http://localhost:8080/actuator/caches
```

---

## 🛠️ Desarrollo

### Estructura del Proyecto
```
src/main/java/com/clinica/mentalhealth/
├── ai/                  # Configuración de IA y Tools
├── config/              # Configuración (Security, OpenAPI, Cache)
├── domain/              # Entidades (User, Patient, Psychologist, etc)
├── repository/          # Repositorios R2DBC
├── security/            # JWT, Filtros, UserPrincipal
├── service/             # Lógica de negocio
└── web/                 # Controladores REST
```

### Comandos Útiles
```bash
# Compilar
./mvnw clean compile

# Ejecutar tests
./mvnw test

# Empaquetar
./mvnw clean package

# Limpiar completamente
./mvnw clean
rm -rf target/
```

---

## 📚 Documentación

- 📖 [MEJORAS_IMPLEMENTADAS.md](MEJORAS_IMPLEMENTADAS.md) - Guía detallada de configuración
- 🆘 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Solución de problemas comunes
- 📋 [RESUMEN_CAMBIOS.md](RESUMEN_CAMBIOS.md) - Resumen de cambios recientes

---

## 🔧 Configuración

### Variables de Entorno
```bash
# Requerida
export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxx

# Opcional (ya tienen valores por defecto)
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=dev
```

### application.properties
```properties
# Base de datos
spring.r2dbc.url=r2dbc:h2:mem:///mental-clinic-db

# Caché
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=30m

# IA
spring.ai.openai.api-key=${DEEPSEEK_API_KEY}
spring.ai.openai.base-url=https://api.deepseek.com
```

---

## 🚦 Estado del Proyecto

- ✅ Configuración unificada
- ✅ Swagger UI sin autenticación
- ✅ Caché implementado
- ✅ IA integrada
- ✅ Logging mejorado
- ✅ Documentación completa

---

## 📝 Licencia

Este proyecto es para fines educativos.

---

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama (`git checkout -b feature/mejora`)
3. Commit tus cambios (`git commit -am 'Agrega mejora'`)
4. Push a la rama (`git push origin feature/mejora`)
5. Abre un Pull Request

---

## 📞 Soporte

Si encuentras problemas:

1. Revisa [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. Verifica los logs de la aplicación
3. Consulta la documentación de Spring Boot

---

**Última actualización**: 26 de Noviembre, 2025  
**Versión**: 0.0.1-SNAPSHOT

