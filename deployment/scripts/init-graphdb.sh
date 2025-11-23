#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# GRAPHDB INITIALIZATION SCRIPT
# 
# Initializes GraphDB by creating the ontomesh repository
# 
# Usage: ./init-graphdb.sh [OPTIONS]
#
# Prerequisites:
# - GraphDB pod must be running
# - kubectl configured and connected to cluster
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
NAMESPACE=${NAMESPACE:-graphdb}
POD_NAME=${POD_NAME:-}
SERVICE_NAME=${SERVICE_NAME:-graphdb}
REPO_CONFIG="$SCRIPT_DIR/graphdb/repo-ontomesh.ttl"
REPO_ID=${REPO_ID:-ontomesh}

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
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
    exit 1
}

usage() {
    cat <<USAGE_EOF
${BLUE}GraphDB Initialization Script${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --namespace NAME       Kubernetes namespace (default: graphdb)
    --pod-name NAME        GraphDB pod name (auto-detected if not provided)
    --service-name NAME    GraphDB service name (default: graphdb)
    --repo-id ID           Repository ID (default: ontomesh)
    --help                 Show this help

${YELLOW}EXAMPLES:${NC}
    # Initialize with defaults:
    $0

    # Specify namespace:
    $0 --namespace graphdb

    # Specify pod and repo:
    $0 --pod-name graphdb-0 --repo-id ontomesh

USAGE_EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        --pod-name)
            POD_NAME="$2"
            shift 2
            ;;
        --service-name)
            SERVICE_NAME="$2"
            shift 2
            ;;
        --repo-id)
            REPO_ID="$2"
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

log "📊 Initializing GraphDB..."
echo "Namespace: $NAMESPACE"
echo "Repository ID: $REPO_ID"
echo "Repository config: $REPO_CONFIG"
echo ""

# Check if repo config file exists
if [ ! -f "$REPO_CONFIG" ]; then
    error "Repository config file not found: $REPO_CONFIG"
fi

# Auto-detect pod name if not provided
if [ -z "$POD_NAME" ]; then
    log "Auto-detecting GraphDB pod..."
    POD_NAME=$(kubectl get pods -n "$NAMESPACE" -l app=graphdb -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    
    if [ -z "$POD_NAME" ]; then
        # Try alternative label
        POD_NAME=$(kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=graphdb -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    fi
    
    if [ -z "$POD_NAME" ]; then
        error "Could not find GraphDB pod in namespace $NAMESPACE"
    fi
    
    success "Found GraphDB pod: $POD_NAME"
fi

# Wait for GraphDB to be ready
log "Waiting for GraphDB to be ready..."
kubectl wait --for=condition=ready pod/"$POD_NAME" -n "$NAMESPACE" --timeout=300s || error "GraphDB pod not ready"
success "GraphDB pod is ready"

# Give GraphDB a bit more time to fully start
log "Waiting for GraphDB service to be fully ready..."
sleep 15

# Check GraphDB health using kubectl exec
log "Checking GraphDB health..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    HEALTH=$(kubectl exec -n "$NAMESPACE" "$POD_NAME" -- curl -s -o /dev/null -w "%{http_code}" http://localhost:7200/rest/repositories 2>/dev/null || echo "000")
    
    if [ "$HEALTH" = "200" ]; then
        success "GraphDB is healthy and responding"
        break
    fi
    
    log "Attempt $attempt/$max_attempts: GraphDB not ready (HTTP $HEALTH), waiting..."
    sleep 5
    attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
    error "GraphDB failed to become healthy within expected time"
fi

# Check if repository already exists
log "Checking if repository '$REPO_ID' already exists..."
REPO_CHECK=$(kubectl exec -n "$NAMESPACE" "$POD_NAME" -- \
    curl -s -o /dev/null -w "%{http_code}" "http://localhost:7200/rest/repositories/$REPO_ID" || echo "000")

if [ "$REPO_CHECK" = "200" ]; then
    warning "Repository '$REPO_ID' already exists, skipping creation"
    success "GraphDB initialization complete"
    exit 0
fi

log "Creating repository '$REPO_ID'..."

# Copy repo config to pod
kubectl cp "$REPO_CONFIG" "$NAMESPACE/$POD_NAME:/tmp/repo-ontomesh.ttl"

# Create the repository
CREATE_RESPONSE=$(kubectl exec -n "$NAMESPACE" "$POD_NAME" -- \
    curl -X POST http://localhost:7200/rest/repositories \
    -H 'Content-Type: multipart/form-data' \
    -F "config=@/tmp/repo-ontomesh.ttl" \
    -w "%{http_code}" \
    -s -o /dev/null)

if [ "$CREATE_RESPONSE" = "201" ] || [ "$CREATE_RESPONSE" = "200" ]; then
    success "Repository '$REPO_ID' created successfully!"
else
    error "Failed to create repository. HTTP response: $CREATE_RESPONSE"
fi

# Verify repository creation
log "Verifying repository creation..."
VERIFY=$(kubectl exec -n "$NAMESPACE" "$POD_NAME" -- \
    curl -s -o /dev/null -w "%{http_code}" "http://localhost:7200/rest/repositories/$REPO_ID")

if [ "$VERIFY" = "200" ]; then
    success "Repository verified successfully!"
else
    error "Failed to verify repository creation"
fi

# Clean up temporary file
kubectl exec -n "$NAMESPACE" "$POD_NAME" -- rm -f /tmp/repo-ontomesh.ttl

success "✅ GraphDB initialization complete!"
echo ""
echo "GraphDB is ready with the '$REPO_ID' repository configured."
echo "Access URL: https://graphdb.ontomesh.com (after DNS/SSL setup)"
echo "Repository endpoint: http://$SERVICE_NAME.$NAMESPACE.svc.cluster.local:7200/repositories/$REPO_ID"
