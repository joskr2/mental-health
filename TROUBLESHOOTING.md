# ❓ SOLUCIÓN DE PROBLEMAS COMUNES

## 🔐 Problema: Swagger UI pide username y password

### ✅ Solución Implementada

Se configuró Spring Security para permitir acceso público a Swagger UI sin autenticación.

**Archivo modificado**: `src/main/java/com/clinica/mentalhealth/config/SecurityConfig.java`

**Rutas públicas configuradas:**
```java
.pathMatchers("/v3/api-docs/**").permitAll()
.pathMatchers("/swagger-ui/**").permitAll()
.pathMatchers("/swagger-ui.html").permitAll()
.pathMatchers("/webjars/**").permitAll()
.pathMatchers("/actuator/**").permitAll()
```

### 📋 Verificación

1. **Reinicia la aplicación**:
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Accede a Swagger UI** (sin login):
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. **Verifica que NO pida credenciales**
   - Deberías ver la interfaz de Swagger directamente
   - Sin ventana de login

---

## ⚠️ Problema: "Unable to resolve table" en el IDE

### ✅ Explicación

Estas son **advertencias del IDE** (IntelliJ/VSCode), NO son errores reales.

**¿Por qué aparecen?**
- H2 es una base de datos **en memoria**
- Las tablas se crean cuando Spring Boot **arranca** (runtime)
- El IDE no puede "ver" las tablas en **tiempo de diseño**
- El código **funciona correctamente** al ejecutarse

### 🔧 Soluciones

**Opción A: Suprimir advertencias** (recomendado)
1. Click derecho en la advertencia amarilla
2. Selecciona: "Suppress for statement" o "Suppress for method"

**Opción B: Ignorar** 
- Las advertencias no afectan la ejecución
- El código funciona correctamente

**Opción C: Configurar DataSource en el IDE** (avanzado)
1. En IntelliJ: View → Tool Windows → Database
2. Agregar nueva conexión H2
3. URL: `jdbc:h2:mem:mental-clinic-db`
4. Username: `sa`
5. Password: (vacío)
6. Nota: Solo funciona mientras la app está corriendo

---

## 🚀 Problema: La aplicación no inicia

### Verificar variable de entorno

**Error típico**:
```
Could not resolve placeholder 'DEEPSEEK_API_KEY'
```

**Solución**:
```bash
export DEEPSEEK_API_KEY=tu_api_key_real
./mvnw spring-boot:run
```

**Verificar que se configuró**:
```bash
echo $DEEPSEEK_API_KEY
```

---

## 📦 Problema: Error de compilación

### Solución 1: Limpiar y recompilar
```bash
./mvnw clean compile
```

### Solución 2: Forzar actualización de dependencias
```bash
./mvnw clean install -U
```

### Solución 3: Eliminar cache de Maven
```bash
rm -rf ~/.m2/repository
./mvnw clean install
```

---

## 🔍 Problema: No veo los logs detallados

### Verificar configuración de logging

**En `application.properties`**:
```properties
logging.level.com.clinica.mentalhealth=DEBUG
logging.level.org.springframework.r2dbc=DEBUG
```

**Ver logs en tiempo real**:
```bash
./mvnw spring-boot:run | grep -E "DEBUG|ERROR|WARN"
```

---

## 💾 Problema: Los datos no persisten

### Explicación

H2 está configurado **en memoria** (`:mem:`), los datos se pierden al reiniciar.

**Esto es INTENCIONAL para desarrollo local.**

**Si necesitas persistencia**:

Cambiar en `application.properties`:
```properties
# De:
spring.r2dbc.url=r2dbc:h2:mem:///mental-clinic-db;...

# A:
spring.r2dbc.url=r2dbc:h2:file:///./data/mental-clinic-db;...
```

---

## 🌐 Problema: No puedo acceder a los endpoints

### Verificar que la app está corriendo

```bash
curl http://localhost:8080/actuator/health
```

**Respuesta esperada**:
```json
{"status":"UP"}
```

### Verificar el puerto

En `application.properties`:
```properties
server.port=8080
```

### Ver todos los endpoints registrados

```bash
curl http://localhost:8080/actuator/mappings | jq
```

---

## 🔐 Problema: JWT Token inválido

### Obtener un token válido

**1. Login**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123"}'
```

**2. Usar el token**:
```bash
TOKEN="el_token_que_recibiste"

curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📊 Problema: El caché no funciona

### Verificar que está habilitado

**En `application.properties`**:
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=30m
```

**En la clase principal** (`MentalHealthApplication.java`):
```java
@EnableCaching  // <- Debe estar presente
```

### Ver estadísticas del caché

```bash
curl http://localhost:8080/actuator/caches
```

---

## 🆘 Comandos Útiles de Emergencia

```bash
# Matar procesos en el puerto 8080
lsof -ti:8080 | xargs kill -9

# Ver procesos Java corriendo
jps -l

# Limpiar completamente el proyecto
./mvnw clean
rm -rf target/

# Verificar versión de Java
java -version

# Debería ser Java 17 o superior
```

---

## 📞 Checklist de Debugging

Antes de buscar ayuda, verifica:

- [ ] ✅ Java 17+ instalado (`java -version`)
- [ ] ✅ Variable `DEEPSEEK_API_KEY` configurada
- [ ] ✅ Puerto 8080 disponible
- [ ] ✅ Proyecto compila sin errores (`./mvnw clean compile`)
- [ ] ✅ Logs muestran "Started MentalHealthApplication"
- [ ] ✅ Actuator health responde: `curl http://localhost:8080/actuator/health`
- [ ] ✅ Swagger UI accesible: `http://localhost:8080/swagger-ui.html`

---

**Última actualización**: 26 de Noviembre, 2025

