#!/bin/bash

# ============================================
# Script de Despliegue en Azure
# ============================================

set -e

echo "☁️  ONDRA - Despliegue en Azure"
echo "=============================="

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuración
RESOURCE_GROUP="ondra-rg"
LOCATION="westeurope"
ACR_NAME="ondraacr"
CONTAINER_NAME="ondra-usuarios-service"
DB_SERVER="ondra-postgres-server"
DB_NAME="Usuarios"
DB_ADMIN="ondraadmin"

# Verificar Azure CLI
if ! command -v az &> /dev/null; then
    echo -e "${RED}❌ Azure CLI no está instalado${NC}"
    echo "Instala desde: https://docs.microsoft.com/cli/azure/install-azure-cli"
    exit 1
fi

# Login a Azure
echo -e "${BLUE}🔐 Verificando sesión de Azure...${NC}"
az account show &> /dev/null || az login

# Menú
echo ""
echo "Selecciona una opción:"
echo "1) 🏗️  Setup inicial (crear recursos)"
echo "2) 🚀 Deploy/Update aplicación"
echo "3) 📊 Ver estado y logs"
echo "4) 🔄 Restart servicio"
echo "5) 🗑️  Eliminar recursos"
echo ""
read -p "Opción: " option

case $option in
    1)
        echo -e "${GREEN}🏗️  Creando recursos en Azure...${NC}"

        # Crear Resource Group
        echo "📦 Creando Resource Group..."
        az group create \
            --name $RESOURCE_GROUP \
            --location $LOCATION

        # Crear Container Registry
        echo "🐳 Creando Container Registry..."
        az acr create \
            --resource-group $RESOURCE_GROUP \
            --name $ACR_NAME \
            --sku Basic \
            --admin-enabled true

        # Crear PostgreSQL
        echo "🐘 Creando PostgreSQL Server..."
        read -sp "Introduce password para PostgreSQL (mín. 8 caracteres): " DB_PASSWORD
        echo ""

        az postgres flexible-server create \
            --resource-group $RESOURCE_GROUP \
            --name $DB_SERVER \
            --location $LOCATION \
            --admin-user $DB_ADMIN \
            --admin-password "$DB_PASSWORD" \
            --sku-name Standard_B1ms \
            --tier Burstable \
            --storage-size 32 \
            --version 16 \
            --public-access 0.0.0.0

        # Crear base de datos
        echo "💾 Creando base de datos..."
        az postgres flexible-server db create \
            --resource-group $RESOURCE_GROUP \
            --server-name $DB_SERVER \
            --database-name $DB_NAME

        echo -e "${GREEN}✅ Recursos creados exitosamente${NC}"
        echo ""
        echo -e "${YELLOW}📝 Guarda estos datos:${NC}"
        echo "Resource Group: $RESOURCE_GROUP"
        echo "ACR: $ACR_NAME.azurecr.io"
        echo "DB Server: $DB_SERVER.postgres.database.azure.com"
        echo "DB Name: $DB_NAME"
        echo "DB User: $DB_ADMIN"
        ;;

    2)
        echo -e "${GREEN}🚀 Desplegando aplicación...${NC}"

        # Login a ACR
        echo "🔐 Login a Container Registry..."
        az acr login --name $ACR_NAME

        # Build y Push
        echo "🔨 Building imagen..."
        docker build -t $ACR_NAME.azurecr.io/usuarios-service:latest .

        echo "📤 Pushing imagen..."
        docker push $ACR_NAME.azurecr.io/usuarios-service:latest

        # Obtener credenciales
        ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv)

        # Crear/Actualizar Container Instance
        echo "🚢 Desplegando container..."

        # Verificar si existe archivo de configuración
        if [ ! -f azure.env ]; then
            echo -e "${YELLOW}⚠️  No se encontró azure.env${NC}"
            echo "Creando archivo de ejemplo..."
            cat > azure.env << EOF
AZURE_POSTGRES_URL=jdbc:postgresql://$DB_SERVER.postgres.database.azure.com:5432/$DB_NAME
AZURE_POSTGRES_USERNAME=$DB_ADMIN
AZURE_POSTGRES_PASSWORD=TU_PASSWORD_AQUI
JWT_SECRET=
GOOGLE_OAUTH_CLIENT_ID=
ENCRYPTION_SECRET_KEY=
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
FRONTEND_URL=
SERVICE_TOKEN=
EOF
            echo -e "${RED}❌ Edita azure.env con tus valores y ejecuta de nuevo${NC}"
            exit 1
        fi

        az container create \
            --resource-group $RESOURCE_GROUP \
            --name $CONTAINER_NAME \
            --image $ACR_NAME.azurecr.io/usuarios-service:latest \
            --registry-login-server $ACR_NAME.azurecr.io \
            --registry-username $ACR_NAME \
            --registry-password $ACR_PASSWORD \
            --dns-name-label ondra-usuarios \
            --ports 8080 \
            --environment-variables-file azure.env \
            --cpu 1 \
            --memory 1 \
            --restart-policy Always || \
        az container restart --resource-group $RESOURCE_GROUP --name $CONTAINER_NAME

        # Obtener URL
        FQDN=$(az container show \
            --resource-group $RESOURCE_GROUP \
            --name $CONTAINER_NAME \
            --query "ipAddress.fqdn" -o tsv)

        echo -e "${GREEN}✅ Despliegue completado${NC}"
        echo ""
        echo "🌐 URL: http://$FQDN:8080"
        echo "🏥 Health: http://$FQDN:8080/actuator/health"
        ;;

    3)
        echo -e "${GREEN}📊 Estado de servicios...${NC}"
        echo ""

        # Estado del container
        az container show \
            --resource-group $RESOURCE_GROUP \
            --name $CONTAINER_NAME \
            --query "{FQDN:ipAddress.fqdn,State:instanceView.state,CPU:containers[0].instanceView.currentState.detailStatus}" \
            --output table

        echo ""
        echo "📋 Logs recientes:"
        az container logs \
            --resource-group $RESOURCE_GROUP \
            --name $CONTAINER_NAME \
            --tail 50

        echo ""
        read -p "¿Ver logs en tiempo real? (y/N): " follow
        if [ "$follow" = "y" ] || [ "$follow" = "Y" ]; then
            az container logs \
                --resource-group $RESOURCE_GROUP \
                --name $CONTAINER_NAME \
                --follow
        fi
        ;;

    4)
        echo -e "${YELLOW}🔄 Reiniciando servicio...${NC}"
        az container restart \
            --resource-group $RESOURCE_GROUP \
            --name $CONTAINER_NAME
        echo -e "${GREEN}✅ Servicio reiniciado${NC}"
        ;;

    5)
        echo -e "${RED}⚠️  ADVERTENCIA: Esto eliminará todos los recursos${NC}"
        read -p "¿Estás seguro? Escribe 'DELETE' para confirmar: " confirm

        if [ "$confirm" = "DELETE" ]; then
            echo -e "${RED}🗑️  Eliminando recursos...${NC}"
            az group delete \
                --name $RESOURCE_GROUP \
                --yes \
                --no-wait
            echo -e "${GREEN}✅ Eliminación iniciada (se completará en background)${NC}"
        else
            echo "Cancelado"
        fi
        ;;

    *)
        echo -e "${RED}Opción inválida${NC}"
        exit 1
        ;;
esac