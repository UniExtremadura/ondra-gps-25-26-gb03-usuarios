#!/bin/bash

# ============================================
# Script de Despliegue Local
# ============================================

set -e

echo "🐳 ONDRA - Despliegue Local del Microservicio de Usuarios"
echo "=========================================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar que existe .env
if [ ! -f .env ]; then
    echo -e "${RED}❌ Error: No se encuentra el archivo .env${NC}"
    echo -e "${YELLOW}💡 Copia .env.example a .env y configura tus variables${NC}"
    echo "   cp .env.example .env"
    exit 1
fi

# Verificar Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker no está instalado${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose no está instalado${NC}"
    exit 1
fi

# Menú de opciones
echo ""
echo "Selecciona una opción:"
echo "1) 🚀 Iniciar servicios (modo producción)"
echo "2) 🛠️  Iniciar servicios (modo desarrollo con PgAdmin)"
echo "3) 🔨 Rebuild y reiniciar"
echo "4) 📊 Ver logs"
echo "5) 🛑 Detener servicios"
echo "6) 🧹 Limpiar todo (⚠️  elimina volúmenes)"
echo "7) ✅ Verificar salud de servicios"
echo ""
read -p "Opción: " option

case $option in
    1)
        echo -e "${GREEN}🚀 Iniciando servicios en modo producción...${NC}"
        docker-compose up -d
        ;;
    2)
        echo -e "${GREEN}🛠️  Iniciando servicios en modo desarrollo...${NC}"
        docker-compose --profile dev up -d
        ;;
    3)
        echo -e "${YELLOW}🔨 Rebuilding...${NC}"
        docker-compose down
        docker-compose build --no-cache
        docker-compose up -d
        ;;
    4)
        echo -e "${GREEN}📊 Mostrando logs (Ctrl+C para salir)...${NC}"
        docker-compose logs -f
        ;;
    5)
        echo -e "${YELLOW}🛑 Deteniendo servicios...${NC}"
        docker-compose down
        ;;
    6)
        read -p "⚠️  Esto eliminará todos los datos. ¿Estás seguro? (y/N): " confirm
        if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
            echo -e "${RED}🧹 Limpiando todo...${NC}"
            docker-compose down -v
            docker system prune -f
        else
            echo "Cancelado"
        fi
        ;;
    7)
        echo -e "${GREEN}✅ Verificando salud de servicios...${NC}"
        echo ""
        echo "📍 PostgreSQL:"
        docker-compose exec postgres pg_isready -U postgres || echo "❌ No disponible"
        echo ""
        echo "📍 Usuarios Service:"
        curl -f http://localhost:8080/actuator/health 2>/dev/null && echo "✅ OK" || echo "❌ No disponible"
        echo ""
        echo "📍 Contenedores activos:"
        docker-compose ps
        ;;
    *)
        echo -e "${RED}Opción inválida${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}✅ Operación completada${NC}"

if [ "$option" = "1" ] || [ "$option" = "2" ]; then
    echo ""
    echo "📍 Servicios disponibles:"
    echo "   - API: http://localhost:8080"
    echo "   - Health: http://localhost:8080/actuator/health"
    echo "   - Metrics: http://localhost:8080/actuator/metrics"
    if [ "$option" = "2" ]; then
        echo "   - PgAdmin: http://localhost:5050"
    fi
    echo ""
    echo "Ver logs: docker-compose logs -f"
fi