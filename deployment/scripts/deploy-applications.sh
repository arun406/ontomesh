#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# ONTOMESH APPLICATION DEPLOYMENT - PHASE 2
# 
# This script deploys application workloads with internal ClusterIP services:
# - Keycloak (identity and access management)
# - GraphDB (RDF triple store)
# - OneRecord services (API server)
#
# Service Type: ClusterIP (internal cluster access only)
# NO external access configured in this phase
#
# NOT CREATED in this phase:
# - Ingress Resources (created in Phase 3)
# - ALB Controller (created in Phase 3)
# - Load Balancer (created in Phase 3)
# - DNS Records (created in Phase 3)
#
# Prerequisites:
# - EKS cluster running (Phase 1 completed)
# - Infrastructure deployed via deploy-infrastructure.sh
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
K8S_DIR="$REPO_ROOT/deployment/k8s"

# Default configuration
AWS_REGION=${AWS_REGION:-ap-south-2}
STORAGE_CLASS=${STORAGE_CLASS:-gp2}
GRAPHDB_SERVICE_TYPE=${GRAPHDB_SERVICE_TYPE:-ClusterIP}
KEYCLOAK_SERVICE_TYPE=${KEYCLOAK_SERVICE_TYPE:-ClusterIP}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    cat <<EOF
${BLUE}Ontomesh Application Deployment${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --aws-region REGION              AWS region (default: ap-south-2)
    --storage-class NAME             Storage class (default: gp2)
    --graphdb-service-type TYPE      GraphDB service type (default: ClusterIP)
    --keycloak-service-type TYPE     Keycloak service type (default: ClusterIP)
    --help                           Show this help

${YELLOW}DEPLOYMENT COMPONENTS:${NC}
    ✅ Namespaces: keycloak, graphdb, ontomesh
    ✅ Keycloak: ClusterIP service on port 8080
    ✅ GraphDB: ClusterIP service on port 7200
    ✅ OneRecord: ClusterIP service on port 8080
    ✅ PersistentVolumeClaims (Keycloak 1Gi, GraphDB 5Gi)
    
    ❌ Ingress Resources (created in Phase 3)
    ❌ Load Balancer (created in Phase 3)
    ❌ Public DNS (created in Phase 3)
    ❌ SSL Certificate (created in Phase 3)
    
${YELLOW}NOTE:${NC}
    - This is Phase 2 of 3-phase deployment
    - Applications are accessible only within the cluster
    - Run post-deployment-dns-ssl.sh for public HTTPS access

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --aws-region)
            AWS_REGION="$2"
            shift 2
            ;;
        --storage-class)
            STORAGE_CLASS="$2"
            shift 2
            ;;
        --graphdb-service-type)
            GRAPHDB_SERVICE_TYPE="$2"
            shift 2
            ;;
        --keycloak-service-type)
            KEYCLOAK_SERVICE_TYPE="$2"
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

# Create required namespaces
create_namespaces() {
    log "📁 Creating Kubernetes namespaces..."
    
    local namespaces=("keycloak" "graphdb" "ontomesh")
    
    for ns in "${namespaces[@]}"; do
        if kubectl get namespace "$ns" >/dev/null 2>&1; then
            warning "Namespace '$ns' already exists"
        else
            kubectl create namespace "$ns"
            success "Created namespace: $ns"
        fi
    done
}

# Deploy Keycloak
deploy_keycloak() {
    log "🔐 Deploying Keycloak..."
    
    # Check if Keycloak is already deployed
    if helm list -n keycloak | grep -q keycloak; then
        warning "Keycloak already deployed, skipping..."
        return 0
    fi
    
    # Use the dedicated Keycloak deployment script
    log "Running dedicated Keycloak deployment script..."
    "$SCRIPT_DIR/deploy-keycloak.sh" --namespace keycloak
    
    # Initialize Keycloak with ontomesh realm
    log "Initializing Keycloak with ontomesh realm..."
    "$SCRIPT_DIR/init-keycloak.sh" --namespace keycloak
    
    success "Keycloak deployed and initialized successfully"
}

# Deploy GraphDB
deploy_graphdb() {
    log "🗄️  Deploying GraphDB..."
    
    # Check if GraphDB is already deployed
    if helm list -n graphdb | grep -q graphdb; then
        warning "GraphDB already deployed, skipping..."
        return 0
    fi
    
    # Use the dedicated GraphDB deployment script
    log "Running dedicated GraphDB deployment script..."
    "$SCRIPT_DIR/deploy-graphdb.sh" --namespace graphdb --storage-class ${STORAGE_CLASS} --pvc-size 5Gi
    
    # Initialize GraphDB with ontomesh repository
    log "Initializing GraphDB with ontomesh repository..."
    "$SCRIPT_DIR/init-graphdb.sh" --namespace graphdb --repo-id ontomesh
    
    success "GraphDB deployed and initialized successfully"
}

# Deploy OneRecord services
deploy_onerecord() {
    log "✈️  Deploying OneRecord services..."
    
    # Check if OneRecord server is already deployed
    if helm list -n ontomesh | grep -q onerecord-server; then
        warning "OneRecord server already deployed, skipping..."
        return 0
    fi
    
    # Get MSK bootstrap brokers from Terraform output
    local MSK_BOOTSTRAP_BROKERS
    cd "$REPO_ROOT/deployment/ontomesh-terraform"
    MSK_BOOTSTRAP_BROKERS=$(terraform output -raw msk_bootstrap_brokers_tls 2>/dev/null || echo "")
    
    if [ -z "$MSK_BOOTSTRAP_BROKERS" ]; then
        warning "Could not retrieve MSK bootstrap brokers from Terraform. Using placeholder."
        MSK_BOOTSTRAP_BROKERS="<MSK_BOOTSTRAP_BROKERS_TLS>"
    fi
    
    log "Installing OneRecord server via Helm chart..."
    
    # Install OneRecord using Helm chart
    helm upgrade --install onerecord-server \
        "$REPO_ROOT/deployment/k8s/charts/onerecord-server" \
        --namespace ontomesh \
        --create-namespace \
        --set onerecordConfig.kafkaBootstrapServers="$MSK_BOOTSTRAP_BROKERS" \
        --set onerecordConfig.repositoryUrl="http://graphdb-service.graphdb.svc.cluster.local:7200" \
        --set onerecordConfig.repositoryName="ontomesh" \
        --set onerecordConfig.issuerUri="http://keycloak-service.keycloak.svc.cluster.local:8080/realms/ontomesh" \
        --set onerecordConfig.serverUrl="http://airline.ontomesh.com" \
        --set service.type="ClusterIP" \
        --wait \
        --timeout 5m
    
    success "OneRecord services deployed successfully"
}

# Verify deployment
verify_deployment() {
    log "🔍 Verifying application deployment..."
    
    echo ""
    echo "=== Deployment Status ==="
    
    # Check namespaces
    echo "Namespaces:"
    kubectl get namespaces keycloak graphdb ontomesh 2>/dev/null || warning "Some namespaces missing"
    
    echo ""
    echo "Helm Releases:"
    helm list -n keycloak 2>/dev/null || warning "No Keycloak Helm releases"
    helm list -n graphdb 2>/dev/null || warning "No GraphDB Helm releases"
    
    echo ""
    echo "Deployments:"
    kubectl get deployments -n keycloak 2>/dev/null || warning "No Keycloak deployments"
    kubectl get deployments -n graphdb 2>/dev/null || warning "No GraphDB deployments"
    kubectl get deployments -n ontomesh 2>/dev/null || warning "No OneRecord deployments"
    
    echo ""
    echo "Services:"
    kubectl get services -n keycloak 2>/dev/null || warning "No Keycloak services"
    kubectl get services -n graphdb 2>/dev/null || warning "No GraphDB services"
    kubectl get services -n ontomesh 2>/dev/null || warning "No OneRecord services"
    
    echo ""
    echo "Ingress:"
    kubectl get ingress -A | grep -E "(keycloak|graphdb|onerecord)" 2>/dev/null || warning "No ingress resources"
    
    echo ""
    echo "Pods:"
    kubectl get pods -A | grep -E "(keycloak|graphdb|onerecord)" || warning "No application pods found"
}

# Main function
main() {
    log "📦 Starting Ontomesh Application Deployment"
    echo "=============================================="
    echo "AWS Region: $AWS_REGION"
    echo "Storage Class: $STORAGE_CLASS"
    echo "GraphDB Service Type: $GRAPHDB_SERVICE_TYPE"
    echo "Keycloak Service Type: $KEYCLOAK_SERVICE_TYPE"
    echo "=============================================="
    echo ""
    
    # Phase 1: Create namespaces
    create_namespaces
    
    # Phase 2: Deploy Keycloak
    deploy_keycloak
    
    # Phase 3: Deploy GraphDB
    deploy_graphdb
    
    # Phase 4: Deploy OneRecord services
    deploy_onerecord
    
    # Phase 5: Verify deployment
    verify_deployment
    
    success "🎉 Phase 2: Application deployment completed!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Deployed Applications (ClusterIP services):"
    echo "  ✅ Keycloak: keycloak-service.keycloak.svc.cluster.local:8080"
    echo "  ✅ GraphDB: graphdb-service.graphdb.svc.cluster.local:7200"
    echo "  ✅ OneRecord: onerecord-server-service.ontomesh.svc.cluster.local:8080"
    echo ""
    echo "Status:"
    echo "  ✅ All services are ClusterIP (internal only)"
    echo "  ❌ NO external access configured yet"
    echo "  ❌ NO ingress resources created"
    echo "  ❌ NO load balancer provisioned"
    echo ""
    echo "Default Credentials:"
    echo "  - Keycloak Admin: admin / admin"
    echo ""
    echo "Next Steps:"
    echo "  1. Verify all pods are Running:"
    echo "     kubectl get pods -A | grep -E '(keycloak|graphdb|onerecord)'"
    echo ""
    echo "  2. Wait for all pods to be ready (check READY column shows 1/1)"
    echo ""
    echo "  3. Run Phase 3 to enable public HTTPS access:"
    echo "     cd $SCRIPT_DIR && ./post-deployment-dns-ssl.sh"
    echo ""
    echo "  Phase 3 will provide access at:"
    echo "     • https://airline.ontomesh.com - OneRecord API"
    echo "     • https://keycloak.ontomesh.com - Keycloak Admin Console"
    echo "     • https://graphdb.ontomesh.com - GraphDB Workbench"
    echo ""
    echo "Troubleshooting:"
    echo "  - View pod status: kubectl get pods -A"
    echo "  - Check pod logs: kubectl logs <pod-name> -n <namespace>"
    echo "  - Describe pod: kubectl describe pod <pod-name> -n <namespace>"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Run main function
main "$@"