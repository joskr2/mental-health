#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m' 

# Configuración de verbosidad
VERBOSE=${VERBOSE:-1}

print_header() {
    echo -e "\n${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

print_debug() {
    if [ "$VERBOSE" -ge 2 ]; then
        echo -e "${GRAY}  → $1${NC}"
    fi
}

print_step() {
    echo -e "${YELLOW}▶ $1${NC}"
}

# Spinner para operaciones largas
spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while ps -p $pid > /dev/null 2>&1; do
        local temp=${spinstr#?}
        printf " ${CYAN}%c${NC}  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

check_docker() {
    print_step "Verificando Docker..."
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker no está corriendo. Por favor, inicia Docker Desktop."
        exit 1
    fi
    print_success "Docker está corriendo"
    print_debug "Docker version: $(docker --version)"
}

# Verificar archivo .env
check_env() {
    print_step "Verificando archivo .env..."
    if [ ! -f .env ]; then
        print_warning "Archivo .env no encontrado. Copiando desde .env.example..."
        cp .env.example .env
        print_warning "Por favor, edita .env con tus credenciales antes de continuar."
        exit 1
    fi
    print_success "Archivo .env encontrado"
    
    # Mostrar variables cargadas (sin valores sensibles)
    if [ "$VERBOSE" -ge 1 ]; then
        source .env 2>/dev/null || true
        print_debug "POSTGRES_DB=${POSTGRES_DB:-mental_clinic}"
        print_debug "POSTGRES_USER=${POSTGRES_USER:-clinic_user}"
        print_debug "DB_PORT=${DB_PORT:-5432}"
        print_debug "APP_PORT=${APP_PORT:-8080}"
    fi
}

# Esperar a que un servicio esté healthy
wait_for_healthy() {
    local container=$1
    local max_attempts=${2:-30}
    local attempt=1
    
    print_step "Esperando a que $container esté listo..."
    
    while [ $attempt -le $max_attempts ]; do
        local status=$(docker inspect --format='{{.State.Health.Status}}' $container 2>/dev/null || echo "not_found")
        
        if [ "$status" = "healthy" ]; then
            print_success "$container está healthy"
            return 0
        elif [ "$status" = "not_found" ]; then
            print_debug "Contenedor no encontrado, esperando... (intento $attempt/$max_attempts)"
        else
            print_debug "Estado: $status (intento $attempt/$max_attempts)"
        fi
        
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$container no alcanzó estado healthy después de $max_attempts intentos"
    return 1
}

# Mostrar logs en tiempo real con colores
show_logs() {
    local profile=$1
    print_info "Mostrando logs en tiempo real (Ctrl+C para salir)"
    echo ""
    docker compose --profile $profile logs -f --tail 100 2>&1 | while IFS= read -r line; do
        # Colorear según el tipo de mensaje
        if echo "$line" | grep -qiE "error|exception|failed|fatal"; then
            echo -e "${RED}$line${NC}"
        elif echo "$line" | grep -qiE "warn"; then
            echo -e "${YELLOW}$line${NC}"
        elif echo "$line" | grep -qiE "started|ready|listening|healthy|success"; then
            echo -e "${GREEN}$line${NC}"
        elif echo "$line" | grep -qiE "info"; then
            echo -e "${CYAN}$line${NC}"
        else
            echo "$line"
        fi
    done
}

case "$1" in
    # ==========================================
    # DESARROLLO
    # ==========================================
    dev)
        print_header "Iniciando entorno de DESARROLLO"
        check_docker
        check_env
        
        print_step "Construyendo imágenes Docker..."
        docker compose --profile dev build
        print_success "Imágenes construidas"
        
        print_step "Iniciando contenedores..."
        docker compose --profile dev up -d
        
        # Esperar a que los servicios estén listos
        wait_for_healthy "mental-health-db" 30
        wait_for_healthy "mental-health-api-dev" 60
        
        print_success "Entorno de desarrollo iniciado"
        echo ""
        echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║       🚀 Servicios disponibles 🚀          ║${NC}"
        echo -e "${GREEN}╠════════════════════════════════════════════╣${NC}"
        echo -e "${GREEN}║${NC}  API:      ${CYAN}http://localhost:8080${NC}           ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}  Swagger:  ${CYAN}http://localhost:8080/swagger-ui.html${NC} ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}  Health:   ${CYAN}http://localhost:8080/actuator/health${NC} ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}  DB:       ${CYAN}localhost:5432${NC}                  ${GREEN}║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
        echo ""
        print_info "Usa './docker.sh dev-logs' para ver logs en tiempo real"
        ;;

    dev-logs)
        print_header "Logs de desarrollo (tiempo real)"
        show_logs "dev"
        ;;

    dev-stop)
        print_header "Deteniendo entorno de desarrollo"
        print_step "Deteniendo contenedores..."
        docker compose --profile dev down
        print_success "Entorno detenido"
        ;;

    # ==========================================
    # DESARROLLO CON LOGS EN VIVO
    # ==========================================
    dev-watch)
        print_header "Iniciando entorno de DESARROLLO con logs en vivo"
        check_docker
        check_env
        
        print_step "Construyendo imágenes Docker..."
        docker compose --profile dev build
        print_success "Imágenes construidas"
        
        print_step "Iniciando contenedores con logs en vivo..."
        print_info "Presiona Ctrl+C para detener"
        echo ""
        
        # Iniciar en foreground para ver logs
        docker compose --profile dev up --build 2>&1 | while IFS= read -r line; do
            if echo "$line" | grep -qiE "error|exception|failed|fatal"; then
                echo -e "${RED}$line${NC}"
            elif echo "$line" | grep -qiE "warn"; then
                echo -e "${YELLOW}$line${NC}"
            elif echo "$line" | grep -qiE "started|ready|listening|healthy|success"; then
                echo -e "${GREEN}$line${NC}"
            elif echo "$line" | grep -qiE "info"; then
                echo -e "${CYAN}$line${NC}"
            else
                echo "$line"
            fi
        done
        ;;

    # ==========================================
    # PRODUCCIÓN
    # ==========================================
    prod)
        print_header "Iniciando entorno de PRODUCCIÓN"
        check_docker
        check_env
        
        print_step "Verificando variables críticas..."
        source .env
        if [ -z "$DEEPSEEK_API_KEY" ] || [ "$DEEPSEEK_API_KEY" = "sk-tu-api-key-aqui" ]; then
            print_error "DEEPSEEK_API_KEY no configurada en .env"
            exit 1
        fi
        print_success "DEEPSEEK_API_KEY configurada"
        
        if [ -z "$JWT_SECRET" ] || [ ${#JWT_SECRET} -lt 32 ]; then
            print_error "JWT_SECRET debe tener al menos 32 caracteres"
            exit 1
        fi
        print_success "JWT_SECRET configurada (${#JWT_SECRET} caracteres)"
        
        print_step "Construyendo imágenes Docker..."
        docker compose --profile prod build
        print_success "Imágenes construidas"
        
        print_step "Iniciando contenedores..."
        docker compose --profile prod up -d
        
        wait_for_healthy "mental-health-db" 30
        wait_for_healthy "mental-health-api-prod" 90
        
        print_success "Entorno de producción iniciado"
        echo ""
        echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║     🔒 Producción - Servicios activos      ║${NC}"
        echo -e "${GREEN}╠════════════════════════════════════════════╣${NC}"
        echo -e "${GREEN}║${NC}  API:      ${CYAN}http://localhost:8080${NC}           ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}  Health:   ${CYAN}http://localhost:8080/actuator/health${NC} ${GREEN}║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
        ;;

    prod-logs)
        print_header "Logs de producción (tiempo real)"
        show_logs "prod"
        ;;

    prod-stop)
        print_header "Deteniendo entorno de producción"
        print_step "Deteniendo contenedores..."
        docker compose --profile prod down
        print_success "Entorno detenido"
        ;;

    # ==========================================
    # BASE DE DATOS
    # ==========================================
    db)
        print_header "Iniciando solo PostgreSQL"
        check_docker
        check_env
        
        print_step "Iniciando contenedor de PostgreSQL..."
        docker compose up postgres -d
        
        wait_for_healthy "mental-health-db" 30
        print_success "PostgreSQL iniciado en localhost:5432"
        ;;

    db-shell)
        print_header "Conectando a PostgreSQL"
        source .env 2>/dev/null || true
        print_info "Conectando como ${POSTGRES_USER:-clinic_user}@${POSTGRES_DB:-mental_clinic}"
        docker exec -it mental-health-db psql -U ${POSTGRES_USER:-clinic_user} -d ${POSTGRES_DB:-mental_clinic}
        ;;

    db-logs)
        print_header "Logs de PostgreSQL (tiempo real)"
        print_info "Presiona Ctrl+C para salir"
        docker compose logs -f postgres
        ;;

    # ==========================================
    # UTILIDADES
    # ==========================================
    status)
        print_header "Estado de los contenedores"
        echo ""
        docker compose ps -a --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
        echo ""
        
        # Mostrar uso de recursos
        print_step "Uso de recursos:"
        docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null || true
        ;;

    clean)
        print_header "Limpiando recursos Docker"
        print_warning "Esto eliminará contenedores, imágenes y volúmenes del proyecto"
        read -p "¿Estás seguro? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_step "Deteniendo contenedores..."
            docker compose --profile dev --profile prod down -v --rmi local
            print_success "Limpieza completada"
        else
            print_info "Cancelado"
        fi
        ;;

    build)
        print_header "Construyendo imagen Docker"
        print_step "Construyendo sin caché..."
        docker compose build --no-cache --progress=plain
        print_success "Imagen construida"
        ;;

    restart)
        print_header "Reiniciando servicios"
        print_step "Reiniciando contenedores..."
        docker compose --profile dev restart
        print_success "Servicios reiniciados"
        ;;

    # ==========================================
    # LOCAL (app sin Docker, BD en Docker)
    # ==========================================
    local)
        print_header "Iniciando en modo LOCAL (app) + PostgreSQL (Docker)"
        check_docker
        check_env
        
        print_step "Iniciando PostgreSQL..."
        docker compose up postgres -d
        
        print_step "Esperando a PostgreSQL..."
        until docker exec mental-health-db pg_isready -U clinic_user > /dev/null 2>&1; do
            sleep 1
        done
        print_success "PostgreSQL listo"
        
        print_step "Iniciando aplicación Spring Boot..."
        print_info "Logs de la aplicación:"
        echo ""
        ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
        ;;

    # ==========================================
    # AYUDA
    # ==========================================
    help|*)
        print_header "Mental Health Clinic - Docker Helper"
        echo "Uso: ./docker.sh [comando]"
        echo ""
        echo -e "${YELLOW}Desarrollo:${NC}"
        echo "  dev           - Iniciar entorno completo (background)"
        echo "  dev-watch     - Iniciar con logs en tiempo real (foreground)"
        echo "  dev-logs      - Ver logs en tiempo real"
        echo "  dev-stop      - Detener entorno de desarrollo"
        echo ""
        echo -e "${YELLOW}Producción:${NC}"
        echo "  prod          - Iniciar entorno de producción"
        echo "  prod-logs     - Ver logs de producción"
        echo "  prod-stop     - Detener entorno de producción"
        echo ""
        echo -e "${YELLOW}Base de Datos:${NC}"
        echo "  db            - Iniciar solo PostgreSQL"
        echo "  db-shell      - Abrir shell de PostgreSQL"
        echo "  db-logs       - Ver logs de PostgreSQL"
        echo ""
        echo -e "${YELLOW}Local (app sin Docker):${NC}"
        echo "  local         - Iniciar app local + PostgreSQL Docker"
        echo ""
        echo -e "${YELLOW}Utilidades:${NC}"
        echo "  status        - Ver estado y recursos de contenedores"
        echo "  build         - Reconstruir imágenes (verbose)"
        echo "  restart       - Reiniciar servicios"
        echo "  clean         - Limpiar todo (contenedores, imágenes, volúmenes)"
        echo "  help          - Mostrar esta ayuda"
        echo ""
        echo -e "${GRAY}Variables de entorno:${NC}"
        echo "  VERBOSE=2     - Mostrar información de debug"
        echo ""
        echo -e "${GRAY}Ejemplos:${NC}"
        echo "  ./docker.sh dev-watch     # Desarrollo con logs en vivo"
        echo "  VERBOSE=2 ./docker.sh dev # Desarrollo con debug"
        ;;
esac
