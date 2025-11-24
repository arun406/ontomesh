#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# ONTOMESH INFRASTRUCTURE DEPLOYMENT - PHASE 1
# 
# This script deploys ONLY core infrastructure components:
# - VPC, Subnets, Internet Gateway, NAT Gateways
# - EKS Cluster and Node Groups
# - MSK Kafka Cluster
# - RDS PostgreSQL Database
# - Security Groups
# - EBS CSI Driver (for persistent volumes)
# - MSK Client IAM Role (IRSA)
#
# NOT CREATED in this phase:
# - ALB Controller (IAM, ServiceAccount, Helm chart)
# - Kubernetes Ingress Resources
# - Application Load Balancer
# - Route53 DNS Records
# - ACM SSL Certificates
#
# These are created in Phase 3 via post-deployment-dns-ssl.sh
#
# Prerequisites:
# - AWS CLI configured with appropriate credentials
# - terraform, kubectl, helm installed
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
TERRAFORM_DIR="$REPO_ROOT/deployment/ontomesh-terraform"

# Default configuration
AWS_REGION=${AWS_REGION:-ap-south-2}
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
    cat <<EOF
${BLUE}Ontomesh Infrastructure Deployment${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --auto-approve            Auto-approve Terraform changes
    --aws-region REGION       AWS region (default: ap-south-2)
    --help                    Show this help

${YELLOW}DEPLOYMENT COMPONENTS:${NC}
    ✅ VPC, Subnets, Gateways, Route Tables
    ✅ EKS Cluster and Worker Nodes
    ✅ MSK Kafka Cluster (2 brokers)
    ✅ RDS PostgreSQL Instance
    ✅ Security Groups
    ✅ EBS CSI Driver (IRSA + Helm)
    ✅ MSK Client IAM Role (IRSA)
    
    ❌ ALB Controller (created in Phase 3)
    ❌ Kubernetes Ingress (created in Phase 3)
    ❌ Route53 DNS (created in Phase 3)
    ❌ ACM SSL Certificate (created in Phase 3)
    
${YELLOW}NOTE:${NC}
    - This is Phase 1 of 3-phase deployment
    - ALB Controller, Ingress, DNS, and SSL are created in Phase 3
    - Run post-deployment-dns-ssl.sh AFTER deploying applications

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
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

# Deploy Terraform infrastructure
deploy_terraform() {
    log "🏗️  Deploying Terraform infrastructure..."
    
    cd "$TERRAFORM_DIR"
    
    # Initialize Terraform
    log "Initializing Terraform..."
    terraform init
    
    # Plan deployment
    log "Planning Terraform deployment..."
    terraform plan -var="aws_region=$AWS_REGION"
    
    # Apply deployment
    if [ "$AUTO_APPROVE" = true ]; then
        log "Applying Terraform configuration (auto-approved)..."
        terraform apply -var="aws_region=$AWS_REGION" -auto-approve
    else
        log "Applying Terraform configuration..."
        terraform apply -var="aws_region=$AWS_REGION"
    fi
    
    success "Terraform infrastructure deployed"
}

# Configure kubectl for EKS
configure_kubectl() {
    log "🔗 Configuring kubectl for EKS cluster..."
    
    local cluster_name="ontomesh-eks"
    
    # Update kubeconfig
    aws eks update-kubeconfig --region "$AWS_REGION" --name "$cluster_name"
    
    # Verify connection
    if kubectl cluster-info >/dev/null 2>&1; then
        success "kubectl configured for cluster: $cluster_name"
    else
        error "Failed to configure kubectl for EKS cluster"
    fi
}

# Install AWS Load Balancer Controller - MOVED TO POST-DEPLOYMENT
# This will be installed after applications are deployed
# See: post-deployment-dns-ssl.sh

# Install EBS CSI Driver
install_ebs_csi_driver() {
    log "💾 Installing EBS CSI Driver..."
    
    # Get EBS CSI IAM role ARN from Terraform
    local ebs_csi_role_arn=$(cd "$TERRAFORM_DIR" && terraform output -raw ebs_csi_role_arn 2>/dev/null)
    
    if [ -z "$ebs_csi_role_arn" ]; then
        error "Failed to get EBS CSI IAM role ARN from Terraform"
    fi
    
    # Check if already installed
    if helm list -n kube-system | grep -q aws-ebs-csi-driver; then
        warning "EBS CSI Driver already installed, upgrading..."
        helm upgrade aws-ebs-csi-driver aws-ebs-csi-driver/aws-ebs-csi-driver \
            --namespace kube-system \
            --set controller.serviceAccount.create=true \
            --set controller.serviceAccount.name=ebs-csi-controller-sa \
            --set controller.serviceAccount.annotations."eks\.amazonaws\.com/role-arn"="$ebs_csi_role_arn"
    else
        # Add Helm repo
        helm repo add aws-ebs-csi-driver https://kubernetes-sigs.github.io/aws-ebs-csi-driver 2>/dev/null || true
        helm repo update
        
        # Install EBS CSI driver with IAM role
        helm install aws-ebs-csi-driver aws-ebs-csi-driver/aws-ebs-csi-driver \
            --namespace kube-system \
            --set controller.serviceAccount.create=true \
            --set controller.serviceAccount.name=ebs-csi-controller-sa \
            --set controller.serviceAccount.annotations."eks\.amazonaws\.com/role-arn"="$ebs_csi_role_arn"
    fi
    
    # Wait for deployment
    log "Waiting for EBS CSI driver to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=aws-ebs-csi-driver -n kube-system --timeout=300s
    
    success "EBS CSI Driver installed (gp2 StorageClass created by Terraform)"
}

# Verify infrastructure
verify_infrastructure() {
    log "🔍 Verifying infrastructure deployment..."
    
    # Check EKS cluster
    if kubectl get nodes >/dev/null 2>&1; then
        success "EKS cluster is accessible"
    else
        error "EKS cluster is not accessible"
    fi
    
    # Check EBS CSI driver
    if kubectl get deployment ebs-csi-controller -n kube-system >/dev/null 2>&1; then
        success "EBS CSI Driver is deployed"
    else
        warning "EBS CSI Driver not found"
    fi
    
    # Show cluster info
    echo ""
    log "Cluster Information:"
    kubectl get nodes
    echo ""
}

# Main function
main() {
    log "🏗️  Starting Phase 1: Core Infrastructure Deployment"
    echo "============================================="
    echo "AWS Region: $AWS_REGION"
    echo "Auto-approve: $AUTO_APPROVE"
    echo "Terraform Directory: $TERRAFORM_DIR"
    echo ""
    echo "Phase 1 Components:"
    echo "  ✅ VPC, Subnets, Internet/NAT Gateways"
    echo "  ✅ EKS Cluster (ontomesh-eks)"
    echo "  ✅ MSK Kafka Cluster (2 brokers)"
    echo "  ✅ RDS PostgreSQL (keycloakdb)"
    echo "  ✅ Security Groups"
    echo "  ✅ EBS CSI Driver"
    echo ""
    echo "NOT created in Phase 1:"
    echo "  ❌ ALB Controller"
    echo "  ❌ Ingress Resources"
    echo "  ❌ Route53 DNS"
    echo "  ❌ ACM SSL Certificate"
    echo "============================================="
    echo ""
    
    # Phase 1: Deploy Terraform infrastructure
    deploy_terraform
    
    # Phase 2: Configure kubectl
    configure_kubectl
    
    # Phase 3: Install EBS CSI Driver
    install_ebs_csi_driver
    
    # Phase 4: Verify everything
    verify_infrastructure
    
    success "🎉 Phase 1: Core Infrastructure deployment completed!"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Deployed:"
    echo "  ✅ VPC and Networking"
    echo "  ✅ EKS Cluster (kubectl configured)"
    echo "  ✅ MSK Kafka Cluster"
    echo "  ✅ RDS PostgreSQL Database"
    echo "  ✅ EBS CSI Driver (storage ready)"
    echo ""
    echo "Next Steps:"
    echo "  1. Run Phase 2: ./deploy-applications.sh"
    echo "     - Deploys Keycloak, GraphDB, OneRecord"
    echo "     - Creates ClusterIP services (internal only)"
    echo ""
    echo "  2. After apps are running, run Phase 3: ./post-deployment-dns-ssl.sh"
    echo "     - Installs ALB Controller"
    echo "     - Creates Ingress resources and ALB"
    echo "     - Configures Route53 DNS"
    echo "     - Issues ACM SSL certificate"
    echo ""
    echo "Verification:"
    echo "  - Check nodes: kubectl get nodes"
    echo "  - Check storage: kubectl get sc"
    echo "  - View EKS: aws eks describe-cluster --name ontomesh-eks --region $AWS_REGION"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Run main function
main "$@"