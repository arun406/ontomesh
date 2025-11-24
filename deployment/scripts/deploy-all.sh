#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# ONTOMESH DEPLOYMENT ORCHESTRATOR
# 
# This is the main deployment script that orchestrates the entire Ontomesh
# infrastructure and application deployment in a modular way.
#
# Usage: ./deploy-all.sh [OPTIONS]
#
# Prerequisites:
# - AWS CLI configured with appropriate credentials
# - terraform, kubectl, helm installed
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

# Default configuration
AWS_REGION=${AWS_REGION:-ap-south-2}
SKIP_INFRA=${SKIP_INFRA:-false}
SKIP_APPS=${SKIP_APPS:-false}
SKIP_DNS=${SKIP_DNS:-false}
AUTO_APPROVE=${AUTO_APPROVE:-false}

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
    cat <<USAGE_EOF
${BLUE}Ontomesh Deployment Script${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --skip-infra              Skip infrastructure deployment (use existing)
    --skip-apps               Skip application deployment
    --skip-dns                Skip DNS/SSL setup (Phase 3)
    --auto-approve            Auto-approve Terraform changes
    --aws-region REGION       AWS region (default: ap-south-2)
    --help                    Show this help

${YELLOW}EXAMPLES:${NC}
    # Full deployment (infrastructure + applications):
    $0

    # Deploy only applications (infrastructure exists):
    $0 --skip-infra

    # Deploy only infrastructure:
    $0 --skip-apps

    # Auto-approve all Terraform changes:
    $0 --auto-approve

    # Use different AWS region:
    $0 --aws-region us-west-2

${YELLOW}DEPLOYMENT PHASES:${NC}
    1. Infrastructure (EKS, MSK, RDS, VPC, EBS CSI Driver)
       - NO ingress, NO ALB Controller, NO DNS/SSL
    2. Applications (Keycloak, GraphDB, OneRecord)
       - ClusterIP services only (no external access)
    3. Public Access (ALB Controller, Ingress, DNS, SSL)
       - Automatically runs after Phase 2 (use --skip-dns to disable)
    
${YELLOW}DOCUMENTATION:${NC}
    For detailed installation guide and troubleshooting, see:
    $REPO_ROOT/deployment/INSTALLATION_GUIDE.md

USAGE_EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-infra)
            SKIP_INFRA=true
            shift
            ;;
        --skip-apps)
            SKIP_APPS=true
            shift
            ;;
        --skip-dns)
            SKIP_DNS=true
            shift
            ;;
        --auto-approve)
            AUTO_APPROVE=true
            shift
            ;;
        --aws-region)
            AWS_REGION="$2"
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

# Verify prerequisites
verify_prerequisites() {
    log "Verifying prerequisites..."
    
    local missing=()
    
    command -v terraform >/dev/null 2>&1 || missing+=("terraform")
    command -v kubectl >/dev/null 2>&1 || missing+=("kubectl")
    command -v helm >/dev/null 2>&1 || missing+=("helm")
    command -v aws >/dev/null 2>&1 || missing+=("aws")
    
    if [ ${#missing[@]} -ne 0 ]; then
        error "Missing required tools: ${missing[*]}"
    fi
    
    # Verify AWS credentials
    if ! aws sts get-caller-identity >/dev/null 2>&1; then
        error "AWS credentials not configured. Run 'aws configure' first."
    fi
    
    success "All prerequisites verified"
}

# Display deployment summary
show_summary() {
    log "Deployment Configuration Summary"
    echo "=================================="
    echo "Repository root: $REPO_ROOT"
    echo "AWS Region: $AWS_REGION"
    echo "Skip Infrastructure: $SKIP_INFRA"
    echo "Skip Applications: $SKIP_APPS"
    echo "Skip DNS/SSL: $SKIP_DNS"
    echo "Auto-approve Terraform: $AUTO_APPROVE"
    echo "=================================="
    echo ""
}

# Main deployment function
main() {
    echo ""
    echo "██████╗ ███╗   ██╗████████╗ ██████╗ ███╗   ███╗███████╗███████╗██╗  ██╗"
    echo "██╔═══██╗████╗  ██║╚══██╔══╝██╔═══██╗████╗ ████║██╔════╝██╔════╝██║  ██║"
    echo "██║   ██║██╔██╗ ██║   ██║   ██║   ██║██╔████╔██║█████╗  ███████╗███████║"
    echo "██║   ██║██║╚██╗██║   ██║   ██║   ██║██║╚██╔╝██║██╔══╝  ╚════██║██╔══██║"
    echo "╚██████╔╝██║ ╚████║   ██║   ╚██████╔╝██║ ╚═╝ ██║███████╗███████║██║  ██║"
    echo " ╚═════╝ ╚═╝  ╚═══╝   ╚═╝    ╚═════╝ ╚═╝     ╚═╝╚══════╝╚══════╝╚═╝  ╚═╝"
    echo ""
    echo "                    🚀 DEPLOYMENT ORCHESTRATOR 🚀"
    echo ""
    
    verify_prerequisites
    show_summary
    
    # Phase 1: Infrastructure Deployment
    if [ "$SKIP_INFRA" = false ]; then
        log "🏗️  Phase 1: Deploying Core Infrastructure (VPC, EKS, MSK, RDS, EBS CSI)..."
        log "           (NO ingress, NO ALB Controller, NO DNS/SSL)"
        if [ "$AUTO_APPROVE" = true ]; then
            "$SCRIPT_DIR/deploy-infrastructure.sh" --auto-approve --aws-region "$AWS_REGION"
        else
            "$SCRIPT_DIR/deploy-infrastructure.sh" --aws-region "$AWS_REGION"
        fi
        success "Infrastructure deployment completed"
    else
        log "⏭️  Skipping infrastructure deployment"
    fi
    
    # Phase 2: Application Deployment
    if [ "$SKIP_APPS" = false ]; then
        log "📦 Phase 2: Deploying Applications (ClusterIP services only)..."
        log "           (Keycloak, GraphDB, OneRecord - internal access only)"
        "$SCRIPT_DIR/deploy-applications.sh" --aws-region "$AWS_REGION"
        success "Application deployment completed"
        echo ""
    else
        log "⏭️  Skipping application deployment"
    fi
    
    # Phase 3: DNS and SSL Setup
    if [ "$SKIP_DNS" = false ]; then
        log "🌐 Phase 3: Setting up Public Access (ALB, Ingress, DNS, SSL)..."
        log "           (This may take 10-15 minutes for certificate validation)"
        echo ""
        
        # Wait a moment for pods to stabilize if apps were just deployed
        if [ "$SKIP_APPS" = false ]; then
            log "Waiting for pods to stabilize..."
            sleep 10
        fi
        
        # Run Phase 3 script
        "$SCRIPT_DIR/post-deployment-dns-ssl.sh"
        success "DNS and SSL setup completed"
        echo ""
    else
        log "⏭️  Skipping DNS/SSL setup (--skip-dns flag)"
        echo ""
        warning "⚠️  IMPORTANT: Applications deployed WITHOUT public access"
        echo ""
        echo "To enable public HTTPS access later:"
        echo "  cd $SCRIPT_DIR && ./post-deployment-dns-ssl.sh"
        echo ""
    fi
    
    # Final summary
    log "🎉 Deployment Complete!"
    echo ""
    echo "=================================="
    echo "        DEPLOYMENT SUMMARY        "
    echo "=================================="
    
    if [ "$SKIP_INFRA" = false ]; then
        echo "✅ Infrastructure: Deployed"
        echo "   - EKS Cluster: ontomesh-eks"
        echo "   - MSK Cluster: ontomesh-msk"
        echo "   - RDS Instance: ontomesh-rds"
        echo "   - EBS CSI Driver: Installed"
    else
        echo "⏭️  Infrastructure: Skipped"
    fi
    
    if [ "$SKIP_APPS" = false ]; then
        echo "✅ Phase 2: Applications Deployed (ClusterIP mode)"
        echo "   - Keycloak: Running (internal port 8080)"
        echo "   - GraphDB: Running (internal port 7200)"
        echo "   - OneRecord: Running (internal port 8080)"
    else
        echo "⏭️  Phase 2: Applications Skipped"
    fi
    
    if [ "$SKIP_DNS" = false ] && [ "$SKIP_APPS" = false ]; then
        echo "✅ Phase 3: Public Access Configured"
        echo "   - ALB Controller: Installed"
        echo "   - Ingress Resources: Created"
        echo "   - Load Balancer: Provisioned"
        echo "   - Route53 DNS: Configured"
        echo "   - SSL Certificate: Issued"
    elif [ "$SKIP_DNS" = false ]; then
        echo "✅ Phase 3: Public Access Configured"
        echo "   - ALB Controller: Installed"
        echo "   - Ingress Resources: Created"
        echo "   - Load Balancer: Provisioned"
        echo "   - Route53 DNS: Configured"
        echo "   - SSL Certificate: Issued"
    else
        echo "⏭️  Phase 3: Public Access Skipped"
    fi
    
    echo ""
    
    if [ "$SKIP_DNS" = false ]; then
        echo "📝 Access Your Applications:"
        echo "   • https://airline.ontomesh.com - OneRecord API"
        echo "   • https://keycloak.ontomesh.com - Keycloak Admin Console (admin/admin)"
        echo "   • https://graphdb.ontomesh.com - GraphDB Workbench"
        echo ""
        echo "⚠️  IMPORTANT: Update your domain registrar nameservers"
        echo "   Run this command to get nameservers:"
        echo "   cd $REPO_ROOT/deployment/ontomesh-terraform && terraform output hosted_zone_name_servers"
        echo ""
        echo "   DNS propagation typically takes 5-30 minutes"
    else
        echo "📝 Next Steps:"
        echo "   1. Verify all pods are Running: kubectl get pods -A"
        echo "   2. Wait for all pods to be ready (1-2 minutes)"
        echo "   3. Run Phase 3 manually: cd $SCRIPT_DIR && ./post-deployment-dns-ssl.sh"
    fi
    echo ""
    echo "🛠️  Troubleshooting:"
    echo "   - Check pods: kubectl get pods -A"
    echo "   - Check services: kubectl get services -A"
    echo "   - View logs: kubectl logs <pod-name> -n <namespace>"
    echo ""
    echo "=================================="
}

# Run main function
main "$@"
