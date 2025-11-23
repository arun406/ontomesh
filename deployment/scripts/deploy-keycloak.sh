#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# KEYCLOAK DEPLOYMENT SCRIPT
# 
# Deploys Keycloak using Bitnami Helm chart with external PostgreSQL (RDS)
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

# Configuration
KEYCLOAK_NAMESPACE=${KEYCLOAK_NAMESPACE:-keycloak}
KEYCLOAK_RELEASE=${KEYCLOAK_RELEASE:-keycloak}
KEYCLOAK_VALUES_FILE="$SCRIPT_DIR/values/keycloak-values.yaml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[Keycloak] $1${NC}"
}

success() {
    echo -e "${GREEN}✓ [Keycloak] $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠ [Keycloak] $1${NC}"
}

error() {
    echo -e "${RED}✗ [Keycloak] $1${NC}"
    exit 1
}

usage() {
    cat <<EOF
${BLUE}Keycloak Deployment Script${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --namespace NAME             Keycloak namespace (default: $KEYCLOAK_NAMESPACE)
    --help                       Show this help

${YELLOW}DESCRIPTION:${NC}
    Deploys Keycloak using Bitnami Helm chart with built-in PostgreSQL database.
    The script uses legacy Docker images for compatibility.

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --namespace)
            KEYCLOAK_NAMESPACE="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1. Use --help for usage information."
            ;;
    esac
done

deploy_keycloak() {
    log "Deploying Keycloak using Bitnami Helm chart..."
    log "Namespace: $KEYCLOAK_NAMESPACE"
    log "Release: $KEYCLOAK_RELEASE"
    
    # Verify prerequisites
    command -v kubectl >/dev/null 2>&1 || error "kubectl is required but not installed"
    command -v helm >/dev/null 2>&1 || error "helm is required but not installed"
    
    # Check if values file exists
    if [ ! -f "$KEYCLOAK_VALUES_FILE" ]; then
        error "Keycloak values file not found at $KEYCLOAK_VALUES_FILE"
    fi
    
    # Create namespace
    log "Creating namespace '$KEYCLOAK_NAMESPACE'..."
    kubectl create namespace "$KEYCLOAK_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    
    # Add/update Helm repository
    log "Adding/updating Bitnami Helm repository..."
    helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
    helm repo update
    
    # Install or upgrade Keycloak
    log "Installing/upgrading Keycloak release '$KEYCLOAK_RELEASE'..."
    helm upgrade --install "$KEYCLOAK_RELEASE" bitnami/keycloak \
        --version 24.4.9 \
        --namespace "$KEYCLOAK_NAMESPACE" \
        --values "$KEYCLOAK_VALUES_FILE" \
        --set image.repository=bitnamilegacy/keycloak \
        --set postgresql.image.repository=bitnamilegacy/postgresql \
        --set global.security.allowInsecureImages=true \
        --wait --timeout 10m
    
    # Wait for deployment
    log "Waiting for Keycloak to be ready..."
    kubectl -n "$KEYCLOAK_NAMESPACE" rollout status statefulset "$KEYCLOAK_RELEASE" --timeout=600s || true
    
    success "Keycloak deployed successfully!"
    
    # Show service information
    echo ""
    log "Keycloak service information:"
    kubectl -n "$KEYCLOAK_NAMESPACE" get svc -l app.kubernetes.io/name=keycloak
    
    echo ""
    log "Keycloak pods:"
    kubectl -n "$KEYCLOAK_NAMESPACE" get pods -l app.kubernetes.io/name=keycloak
    
    echo ""
    log "Keycloak admin credentials:"
    echo "  Username: admin"
    echo "  Password: \$(kubectl get secret --namespace $KEYCLOAK_NAMESPACE $KEYCLOAK_RELEASE -o jsonpath=\"{.data.admin-password}\" | base64 -d)"
    
    echo ""
    success "Keycloak is ready for use!"
    warning "Configure ingress resources to access Keycloak externally"
}

# Main execution
deploy_keycloak