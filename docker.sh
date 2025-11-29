#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' 

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

check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker no está corriendo. Por favor, inicia Docker Desktop."
        exit 1
    fi
    print_success "Docker está corriendo"
}

# Verificar archivo .env
check_env() {
    if [ ! -f .env ]; then
        print_warning "Archivo .env no encontrado. Copiando desde .env.example..."
        cp .env.example .env
        print_warning "Por favor, edita .env con tus credenciales antes de continuar."
        exit 1
    fi
    print_success "Archivo .env encontrado"
}

case "$1" in
    # ==========================================
    # DESARROLLO
    # ==========================================
    dev)
        print_header "Iniciando entorno de DESARROLLO"
        check_docker
        check_env
        docker compose --profile dev up -d --build
        print_success "Entorno de desarrollo iniciado"
        echo -e "\n${GREEN}Servicios disponibles:${NC}"
        echo "  - API:      http://localhost:8080"
        echo "  - Swagger:  http://localhost:8080/swagger-ui.html"
        echo "  - Health:   http://localhost:8080/actuator/health"
        echo "  - DB:       localhost:5432"
        ;;

    dev-logs)
        print_header "Logs de desarrollo"
        docker compose --profile dev logs -f
        ;;

    dev-stop)
        print_header "Deteniendo entorno de desarrollo"
        docker compose --profile dev down
        print_success "Entorno detenido"
        ;;

    # ==========================================
    # PRODUCCIÓN
    # ==========================================
    prod)
        print_header "Iniciando entorno de PRODUCCIÓN"
        check_docker
        check_env
        
        # Verificar variables críticas
        source .env
        if [ -z "$DEEPSEEK_API_KEY" ] || [ "$DEEPSEEK_API_KEY" = "sk-tu-api-key-aqui" ]; then
            print_error "DEEPSEEK_API_KEY no configurada en .env"
            exit 1
        fi
        if [ -z "$JWT_SECRET" ] || [ ${#JWT_SECRET} -lt 32 ]; then
            print_error "JWT_SECRET debe tener al menos 32 caracteres"
            exit 1
        fi
        
        docker compose --profile prod up -d --build
        print_success "Entorno de producción iniciado"
        echo -e "\n${GREEN}Servicios disponibles:${NC}"
        echo "  - API:      http://localhost:8080"
        echo "  - Health:   http://localhost:8080/actuator/health"
        ;;

    prod-logs)
        print_header "Logs de producción"
        docker compose --profile prod logs -f
        ;;

    prod-stop)
        print_header "Deteniendo entorno de producción"
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
        docker compose up postgres -d
        print_success "PostgreSQL iniciado en localhost:5432"
        ;;

    db-shell)
        print_header "Conectando a PostgreSQL"
        source .env 2>/dev/null || true
        docker exec -it mental-health-db psql -U ${POSTGRES_USER:-clinic_user} -d ${POSTGRES_DB:-mental_clinic}
        ;;

    db-logs)
        docker compose logs -f postgres
        ;;

    # ==========================================
    # UTILIDADES
    # ==========================================
    status)
        print_header "Estado de los contenedores"
        docker compose ps -a
        ;;

    clean)
        print_header "Limpiando recursos Docker"
        print_warning "Esto eliminará contenedores, imágenes y volúmenes del proyecto"
        read -p "¿Estás seguro? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose --profile dev --profile prod down -v --rmi local
            print_success "Limpieza completada"
        else
            echo "Cancelado"
        fi
        ;;

    build)
        print_header "Construyendo imagen Docker"
        docker compose build --no-cache
        print_success "Imagen construida"
        ;;

    # ==========================================
    # LOCAL (app sin Docker, BD en Docker)
    # ==========================================
    local)
        print_header "Iniciando en modo LOCAL (app) + PostgreSQL (Docker)"
        check_docker
        check_env
        # Iniciar solo la BD
        docker compose up postgres -d
        # Esperar a que esté lista
        echo "Esperando a PostgreSQL..."
        until docker exec mental-health-db pg_isready -U clinic_user > /dev/null 2>&1; do
            sleep 1
        done
        print_success "PostgreSQL listo"
        # Iniciar app con perfil dev
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
        echo "  dev           - Iniciar entorno completo de desarrollo"
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
        echo "  status        - Ver estado de contenedores"
        echo "  build         - Reconstruir imágenes"
        echo "  clean         - Limpiar todo (contenedores, imágenes, volúmenes)"
        echo "  help          - Mostrar esta ayuda"
        ;;
esac
