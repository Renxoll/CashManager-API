#!/usr/bin/env bash
# Despliegue de SmartCash en Azure Container Apps (ACA), construyendo la imagen en la nube
# con ACR Tasks a partir del Dockerfile local. Pensado para correrse paso a paso (o como
# script) desde la raíz del repo, donde vive el Dockerfile.
set -euo pipefail

# ---------------------------------------------------------------------------
# 1. Variables
# ---------------------------------------------------------------------------
RESOURCE_GROUP="rg-smartcash-prod"
LOCATION="eastus"
# El nombre de un ACR es global en todo Azure (pasa a formar parte de su hostname
# *.azurecr.io) -- agrega un sufijo propio si "smartcashacr" ya está tomado.
ACR_NAME="smartcashacr"
ENV_NAME="smartcash-env"
APP_NAME="smartcash-api"
IMAGE_TAG="v1" # en un pipeline real: el SHA del commit o el número de build, no un literal fijo

# Datos de conexión a Postgres: se asume un servidor ya existente (Azure Database for
# PostgreSQL Flexible Server u otro proveedor administrado) -- provisionarlo no es parte de
# este flujo, solo referenciarlo. Ajusta estos 3 valores a tu instancia real.
DB_HOST="smartcash-pg.postgres.database.azure.com"
DB_NAME="smartcash"
DB_USERNAME="smartcash_admin"
DB_PASSWORD="CAMBIAR-ESTO-antes-de-correr"   # nunca lo dejes hardcodeado en un script versionado

# Secretos de aplicación -- mismo comentario: reemplázalos o expórtalos como variables de
# entorno antes de correr el script (ej. desde Key Vault en un pipeline de CI/CD real).
IAM_TOKEN_SECRET="CAMBIAR-ESTO-secreto-hmac-largo-y-aleatorio"
LLM_API_KEY="CAMBIAR-ESTO-api-key-de-openai"

# ---------------------------------------------------------------------------
# 2. Resource Group + Azure Container Registry (SKU Basic)
# ---------------------------------------------------------------------------
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION"

# --admin-enabled se omite a propósito (default: false). En el paso 6 la app se autentica
# contra el ACR con su propia identidad administrada, no con usuario/password de admin.
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic

ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)

# ---------------------------------------------------------------------------
# 3. Build remoto: sube el contexto local (respeta .dockerignore) y construye la imagen
#    dentro de Azure via ACR Tasks -- no depende de tener Docker corriendo localmente.
# ---------------------------------------------------------------------------
az acr build \
  --registry "$ACR_NAME" \
  --image "$APP_NAME:$IMAGE_TAG" \
  --file Dockerfile \
  .

# ---------------------------------------------------------------------------
# 4. Entorno de Container Apps
# ---------------------------------------------------------------------------
# Primer uso de Container Apps en la suscripción: registra el resource provider si hace
# falta (no-op si ya está registrado, así que es seguro dejarlo siempre en el script).
az provider register --namespace Microsoft.App --wait
az provider register --namespace Microsoft.OperationalInsights --wait

az containerapp env create \
  --name "$ENV_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION"

# ---------------------------------------------------------------------------
# 5. Crear y desplegar la Container App
# ---------------------------------------------------------------------------
# --registry-identity system: ACA crea una identidad administrada system-assigned para la
# app y le otorga el rol AcrPull sobre el registry automáticamente -- sin usuario/password
# de ACR circulando por variables de entorno ni por el historial de despliegues.
#
# --secrets / secretref:  los 3 valores sensibles (password de BD, secreto HMAC de IAM, API
# key del LLM) se guardan como secrets nativos de ACA y se referencian en --env-vars con
# secretref:<nombre> -- nunca quedan como texto plano en `az containerapp show` ni en logs
# de despliegue, a diferencia de pasarlos directo como env var.
az containerapp create \
  --name "$APP_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENV_NAME" \
  --image "$ACR_LOGIN_SERVER/$APP_NAME:$IMAGE_TAG" \
  --target-port 8080 \
  --ingress external \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-identity system \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --secrets \
      db-password="$DB_PASSWORD" \
      iam-token-secret="$IAM_TOKEN_SECRET" \
      llm-api-key="$LLM_API_KEY" \
  --env-vars \
      SPRING_PROFILES_ACTIVE=prod \
      SPRING_DATASOURCE_URL="jdbc:postgresql://$DB_HOST:5432/$DB_NAME?sslmode=require" \
      SPRING_DATASOURCE_USERNAME="$DB_USERNAME" \
      SPRING_DATASOURCE_PASSWORD=secretref:db-password \
      IAM_TOKEN_SECRET=secretref:iam-token-secret \
      LLM_API_KEY=secretref:llm-api-key \
      FCM_ENABLED=false

# ---------------------------------------------------------------------------
# Verificación
# ---------------------------------------------------------------------------
APP_URL=$(az containerapp show \
  --name "$APP_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --query properties.configuration.ingress.fqdn -o tsv)

echo "Desplegado en: https://$APP_URL"
echo "Health check:  curl https://$APP_URL/actuator/health"

# Nota sobre --min-replicas 1: evita el cold start que tuviste que resolver con un ping de
# keep-alive en el plan free de Render. Si el costo importa más que la latencia del primer
# request tras estar inactivo, --min-replicas 0 escala a cero y solo cobra por uso real.
