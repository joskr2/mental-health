#!/bin/bash
# Script de inicio rapido para Mental Health Clinic API
# Uso: ./start.sh

echo "🚀 Iniciando Mental Health Clinic API..."
echo ""

# Verificar Java
echo "📋 Verificando Java..."
if ! command -v java &> /dev/null; then
    echo "❌ Java no encontrado. Por favor instala Java 17 o superior."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Se requiere Java 17 o superior. Version actual: $JAVA_VERSION"
    exit 1
fi
echo "✅ Java $JAVA_VERSION encontrado"
echo ""

# Verificar API Key
echo "📋 Verificando DEEPSEEK_API_KEY..."
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "⚠️  DEEPSEEK_API_KEY no configurada"
    echo ""
    read -p "Ingresa tu DEEPSEEK_API_KEY: " api_key
    export DEEPSEEK_API_KEY="$api_key"
    echo "✅ API Key configurada"
else
    echo "✅ API Key encontrada"
fi
echo ""

# Verificar puerto 8080
echo "📋 Verificando puerto 8080..."
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️  Puerto 8080 ya está en uso"
    read -p "¿Deseas matar el proceso? (s/n): " kill_process
    if [ "$kill_process" = "s" ]; then
        lsof -ti:8080 | xargs kill -9
        echo "✅ Proceso terminado"
    else
        echo "❌ No se puede iniciar la aplicación"
        exit 1
    fi
else
    echo "✅ Puerto 8080 disponible"
fi
echo ""

# Compilar (opcional)
echo "📋 ¿Deseas compilar el proyecto?"
read -p "(s/n): " compile
if [ "$compile" = "s" ]; then
    echo "🔨 Compilando..."
    ./mvnw clean compile -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Error al compilar"
        exit 1
    fi
    echo "✅ Compilación exitosa"
    echo ""
fi

# Iniciar aplicación
echo "🚀 Iniciando aplicación..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📖 Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "💚 Health Check: http://localhost:8080/actuator/health"
echo "📊 Métricas:     http://localhost:8080/actuator/metrics"
echo ""
echo "👤 Usuarios de prueba:"
echo "   - admin/123 (ROLE_ADMIN)"
echo "   - doc/123   (ROLE_PSYCHOLOGIST)"
echo "   - pepe/123  (ROLE_PATIENT)"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Presiona Ctrl+C para detener"
echo ""

./mvnw spring-boot:run

