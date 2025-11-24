#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# UNINSTALL APPLICATIONS
# 
# This script uninstalls all deployed applications from the Kubernetes cluster:
# - Keycloak
# - GraphDB
# - OneRecord Server/Client/Processor
#
# Usage: ./uninstall-applications.sh [OPTIONS]
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

success() {
    echo -e "${GREEN}✓ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

error() {
    echo -e "${RED}✗ $1${NC}"
}

usage() {
    cat <<USAGE_EOF
${BLUE}Uninstall Ontomesh Applications${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --skip-keycloak       Skip Keycloak uninstallation
    --skip-graphdb        Skip GraphDB uninstallation
    --skip-onerecord      Skip OneRecord uninstallation
    --delete-namespaces   Delete namespaces after uninstallation
    --delete-pvcs         Delete Persistent Volume Claims
    --help                Show this help

${YELLOW}EXAMPLES:${NC}
    # Uninstall all applications (keep namespaces and PVCs):
    $0

    # Uninstall and delete everything:
    $0 --delete-namespaces --delete-pvcs

    # Uninstall only OneRecord:
    $0 --skip-keycloak --skip-graphdb

USAGE_EOF
}

# Default configuration
SKIP_KEYCLOAK=false
SKIP_GRAPHDB=false
SKIP_ONERECORD=false
DELETE_NAMESPACES=false
DELETE_PVCS=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-keycloak)
            SKIP_KEYCLOAK=true
            shift
            ;;
        --skip-graphdb)
            SKIP_GRAPHDB=true
            shift
            ;;
        --skip-onerecord)
            SKIP_ONERECORD=true
            shift
            ;;
        --delete-namespaces)
            DELETE_NAMESPACES=true
            shift
            ;;
        --delete-pvcs)
            DELETE_PVCS=true
            shift
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1. Use --help for usage information."
            exit 1
            ;;
    esac
done

log "🗑️  Starting Application Uninstallation"
echo "=========================================="
echo "Skip Keycloak: $SKIP_KEYCLOAK"
echo "Skip GraphDB: $SKIP_GRAPHDB"
echo "Skip OneRecord: $SKIP_ONERECORD"
echo "Delete Namespaces: $DELETE_NAMESPACES"
echo "Delete PVCs: $DELETE_PVCS"
echo "=========================================="
echo ""

# Uninstall Keycloak
if [ "$SKIP_KEYCLOAK" = false ]; then
    log "🔐 Uninstalling Keycloak..."
    
    # Check if Helm release exists
    if helm list -n keycloak | grep -q keycloak; then
        log "Uninstalling Keycloak Helm release..."
        helm uninstall keycloak -n keycloak
        success "Keycloak Helm release uninstalled"
    else
        warning "No Keycloak Helm release found"
    fi
    
    # Delete any remaining resources
    log "Cleaning up remaining Keycloak resources..."
    kubectl delete statefulset --all -n keycloak 2>/dev/null || true
    kubectl delete deployment --all -n keycloak 2>/dev/null || true
    kubectl delete service --all -n keycloak 2>/dev/null || true
    kubectl delete configmap --all -n keycloak 2>/dev/null || true
    kubectl delete secret --all -n keycloak 2>/dev/null || true
    
    if [ "$DELETE_PVCS" = true ]; then
        log "Deleting Keycloak PVCs..."
        kubectl delete pvc --all -n keycloak 2>/dev/null || true
        success "Keycloak PVCs deleted"
    fi
    
    success "Keycloak uninstalled"
else
    log "⏭️  Skipping Keycloak uninstallation"
fi

echo ""

# Uninstall GraphDB
if [ "$SKIP_GRAPHDB" = false ]; then
    log "📊 Uninstalling GraphDB..."
    
    # Check if Helm release exists
    if helm list -n graphdb | grep -q graphdb; then
        log "Uninstalling GraphDB Helm release..."
        helm uninstall graphdb -n graphdb
        success "GraphDB Helm release uninstalled"
    else
        warning "No GraphDB Helm release found"
    fi
    
    # Delete any remaining resources
    log "Cleaning up remaining GraphDB resources..."
    kubectl delete statefulset --all -n graphdb 2>/dev/null || true
    kubectl delete deployment --all -n graphdb 2>/dev/null || true
    kubectl delete service --all -n graphdb 2>/dev/null || true
    kubectl delete configmap --all -n graphdb 2>/dev/null || true
    kubectl delete secret --all -n graphdb 2>/dev/null || true
    
    if [ "$DELETE_PVCS" = true ]; then
        log "Deleting GraphDB PVCs..."
        kubectl delete pvc --all -n graphdb 2>/dev/null || true
        success "GraphDB PVCs deleted"
    fi
    
    success "GraphDB uninstalled"
else
    log "⏭️  Skipping GraphDB uninstallation"
fi

echo ""

# Uninstall OneRecord
if [ "$SKIP_ONERECORD" = false ]; then
    log "📦 Uninstalling OneRecord..."
    
    # Check if Helm release exists
    if helm list -n ontomesh | grep -q onerecord-server; then
        log "Uninstalling OneRecord Helm release..."
        helm uninstall onerecord-server -n ontomesh
        success "OneRecord Helm release uninstalled"
    else
        warning "No OneRecord Helm release found"
    fi
    
    # Delete any remaining resources
    log "Cleaning up remaining OneRecord resources..."
    kubectl delete deployment --all -n ontomesh 2>/dev/null || true
    kubectl delete statefulset --all -n ontomesh 2>/dev/null || true
    kubectl delete service --all -n ontomesh 2>/dev/null || true
    kubectl delete configmap --all -n ontomesh 2>/dev/null || true
    kubectl delete secret --all -n ontomesh 2>/dev/null || true
    
    if [ "$DELETE_PVCS" = true ]; then
        log "Deleting OneRecord PVCs..."
        kubectl delete pvc --all -n ontomesh 2>/dev/null || true
        success "OneRecord PVCs deleted"
    fi
    
    success "OneRecord uninstalled"
else
    log "⏭️  Skipping OneRecord uninstallation"
fi

echo ""

# Delete namespaces if requested
if [ "$DELETE_NAMESPACES" = true ]; then
    log "🗑️  Deleting namespaces..."
    
    if [ "$SKIP_KEYCLOAK" = false ]; then
        log "Deleting keycloak namespace..."
        kubectl delete namespace keycloak 2>/dev/null || warning "keycloak namespace not found"
    fi
    
    if [ "$SKIP_GRAPHDB" = false ]; then
        log "Deleting graphdb namespace..."
        kubectl delete namespace graphdb 2>/dev/null || warning "graphdb namespace not found"
    fi
    
    if [ "$SKIP_ONERECORD" = false ]; then
        log "Deleting ontomesh namespace..."
        kubectl delete namespace ontomesh 2>/dev/null || warning "ontomesh namespace not found"
    fi
    
    success "Namespaces deleted"
fi

echo ""
log "✅ Application uninstallation complete!"
echo ""
echo "=========================================="
echo "         UNINSTALLATION SUMMARY          "
echo "=========================================="

if [ "$SKIP_KEYCLOAK" = false ]; then
    echo "✅ Keycloak: Uninstalled"
else
    echo "⏭️  Keycloak: Skipped"
fi

if [ "$SKIP_GRAPHDB" = false ]; then
    echo "✅ GraphDB: Uninstalled"
else
    echo "⏭️  GraphDB: Skipped"
fi

if [ "$SKIP_ONERECORD" = false ]; then
    echo "✅ OneRecord: Uninstalled"
else
    echo "⏭️  OneRecord: Skipped"
fi

echo ""

if [ "$DELETE_NAMESPACES" = true ]; then
    echo "✅ Namespaces: Deleted"
else
    echo "⚠️  Namespaces: Preserved (use --delete-namespaces to remove)"
fi

if [ "$DELETE_PVCS" = true ]; then
    echo "✅ PVCs: Deleted"
else
    echo "⚠️  PVCs: Preserved (use --delete-pvcs to remove)"
fi

echo ""
echo "🔍 Verification:"
echo "   kubectl get pods -A"
echo "   kubectl get namespaces"
echo "   kubectl get pvc -A"
echo ""
echo "=========================================="
