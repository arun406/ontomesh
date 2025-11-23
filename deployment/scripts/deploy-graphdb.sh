#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# GRAPHDB DEPLOYMENT SCRIPT
# 
# Deploys GraphDB using Helm chart with proper configuration
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

# Configuration
GRAPHDB_NAMESPACE=${GRAPHDB_NAMESPACE:-graphdb}
GRAPHDB_RELEASE=${GRAPHDB_RELEASE:-graphdb}
STORAGE_CLASS=${STORAGE_CLASS:-gp2}
GRAPHDB_PVC_SIZE=${GRAPHDB_PVC_SIZE:-5Gi}
GRAPHDB_HELM_REPO=${GRAPHDB_HELM_REPO:-https://helm.ontotext.com}
GRAPHDB_HELM_CHART=${GRAPHDB_HELM_CHART:-ontotext/graphdb}
GRAPHDB_VALUES_FILE="$SCRIPT_DIR/values/graphdb-values.yaml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[GraphDB] $1${NC}"
}

success() {
    echo -e "${GREEN}✓ [GraphDB] $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠ [GraphDB] $1${NC}"
}

error() {
    echo -e "${RED}✗ [GraphDB] $1${NC}"
    exit 1
}

usage() {
    cat <<EOF
${BLUE}GraphDB Deployment Script${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --namespace NAME             GraphDB namespace (default: $GRAPHDB_NAMESPACE)
    --storage-class NAME         Storage class (default: $STORAGE_CLASS)
    --pvc-size SIZE              PVC size (default: $GRAPHDB_PVC_SIZE)
    --help                       Show this help

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --namespace)
            GRAPHDB_NAMESPACE="$2"
            shift 2
            ;;
        --storage-class)
            STORAGE_CLASS="$2"
            shift 2
            ;;
        --pvc-size)
            GRAPHDB_PVC_SIZE="$2"
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

deploy_graphdb() {
    log "Deploying GraphDB using Helm..."
    log "Namespace: $GRAPHDB_NAMESPACE"
    log "Release: $GRAPHDB_RELEASE"
    log "Storage Class: $STORAGE_CLASS"
    log "PVC Size: $GRAPHDB_PVC_SIZE"
    
    # Verify prerequisites
    command -v kubectl >/dev/null 2>&1 || error "kubectl is required but not installed"
    command -v helm >/dev/null 2>&1 || error "helm is required but not installed"
    
    # Check if values file exists
    if [ ! -f "$GRAPHDB_VALUES_FILE" ]; then
        error "GraphDB values file not found at $GRAPHDB_VALUES_FILE"
    fi
    
    # Create namespace
    log "Creating namespace '$GRAPHDB_NAMESPACE'..."
    kubectl create namespace "$GRAPHDB_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    
    # Add/update Helm repository
    log "Adding/updating Helm repository..."
    helm repo add ontotext "$GRAPHDB_HELM_REPO" 2>/dev/null || true
    helm repo update
    
    # Install or upgrade GraphDB
    log "Installing/upgrading GraphDB release '$GRAPHDB_RELEASE'..."
    helm upgrade --install "$GRAPHDB_RELEASE" "$GRAPHDB_HELM_CHART" \
        --namespace "$GRAPHDB_NAMESPACE" \
        --values "$GRAPHDB_VALUES_FILE" \
        --set storageClass="$STORAGE_CLASS" \
        --set persistence.size="$GRAPHDB_PVC_SIZE" \
        --wait --timeout 10m
    
    # Wait for deployment
    log "Waiting for GraphDB to be ready..."
    kubectl -n "$GRAPHDB_NAMESPACE" rollout status statefulset "$GRAPHDB_RELEASE" --timeout=300s
    
    success "GraphDB deployed successfully!"
    
    # Show service information
    echo ""
    log "GraphDB service information:"
    kubectl -n "$GRAPHDB_NAMESPACE" get svc -l app.kubernetes.io/name=graphdb
    
    echo ""
    log "GraphDB pods:"
    kubectl -n "$GRAPHDB_NAMESPACE" get pods -l app.kubernetes.io/name=graphdb
    
    echo ""
    success "GraphDB is ready for use!"
    warning "If you need to initialize GraphDB repositories, run the init script"
}

# Main execution
deploy_graphdb