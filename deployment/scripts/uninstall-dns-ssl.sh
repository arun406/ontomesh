#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# UNINSTALL DNS, SSL, AND LOAD BALANCER
# 
# This script uninstalls resources created by post-deployment-dns-ssl.sh:
# - AWS Load Balancer Controller (Helm chart)
# - Kubernetes Ingress resources
# - Application Load Balancer (ALB)
# - Route53 hosted zone and DNS records
# - ACM SSL certificates
# - IAM roles and policies
#
# Usage: ./uninstall-dns-ssl.sh [OPTIONS]
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
TERRAFORM_DIR="$REPO_ROOT/deployment/ontomesh-terraform"
AWS_REGION=${AWS_REGION:-ap-south-2}

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
${BLUE}Uninstall Ontomesh DNS, SSL, and Load Balancer${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --skip-alb-controller   Skip uninstalling AWS Load Balancer Controller
    --skip-ingress          Skip deleting Kubernetes ingress resources
    --skip-dns              Skip deleting Route53 DNS and ACM certificate
    --keep-iam-roles        Keep IAM roles and policies (preserve for reuse)
    --auto-approve          Skip confirmation prompts
    --help                  Show this help

${YELLOW}EXAMPLES:${NC}
    # Uninstall everything with confirmation:
    $0

    # Uninstall without prompts:
    $0 --auto-approve

    # Uninstall ALB Controller but keep DNS and IAM roles:
    $0 --skip-dns --keep-iam-roles

    # Uninstall only ingress resources:
    $0 --skip-alb-controller --skip-dns

${YELLOW}NOTES:${NC}
    - This script uses Terraform to destroy resources
    - ALB will be automatically deleted when ingress resources are removed
    - DNS records will be deleted when Route53 hosted zone is destroyed
    - ACM certificate will be deleted automatically with Route53
    - IAM roles can be preserved for future use with --keep-iam-roles

USAGE_EOF
}

# Default configuration
SKIP_ALB_CONTROLLER=false
SKIP_INGRESS=false
SKIP_DNS=false
KEEP_IAM_ROLES=false
AUTO_APPROVE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-alb-controller)
            SKIP_ALB_CONTROLLER=true
            shift
            ;;
        --skip-ingress)
            SKIP_INGRESS=true
            shift
            ;;
        --skip-dns)
            SKIP_DNS=true
            shift
            ;;
        --keep-iam-roles)
            KEEP_IAM_ROLES=true
            shift
            ;;
        --auto-approve)
            AUTO_APPROVE=true
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

log "🗑️  Starting DNS/SSL/Load Balancer Uninstallation"
echo "=========================================="
echo "Skip ALB Controller: $SKIP_ALB_CONTROLLER"
echo "Skip Ingress: $SKIP_INGRESS"
echo "Skip DNS: $SKIP_DNS"
echo "Keep IAM Roles: $KEEP_IAM_ROLES"
echo "Auto Approve: $AUTO_APPROVE"
echo "=========================================="
echo ""

# Confirmation prompt
if [ "$AUTO_APPROVE" = false ]; then
    warning "This will delete:"
    [ "$SKIP_ALB_CONTROLLER" = false ] && echo "  - AWS Load Balancer Controller (Helm release)"
    [ "$SKIP_INGRESS" = false ] && echo "  - Kubernetes Ingress resources (ALB will be deleted)"
    [ "$SKIP_DNS" = false ] && echo "  - Route53 hosted zone and DNS records"
    [ "$SKIP_DNS" = false ] && echo "  - ACM SSL certificate"
    [ "$KEEP_IAM_ROLES" = false ] && echo "  - ALB Controller IAM role and policies"
    echo ""
    read -p "Are you sure you want to proceed? (yes/no): " -r
    echo
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log "Uninstallation cancelled"
        exit 0
    fi
fi

# Step 1: Uninstall AWS Load Balancer Controller
if [ "$SKIP_ALB_CONTROLLER" = false ]; then
    log "🔧 Uninstalling AWS Load Balancer Controller..."
    
    # Check if Helm release exists
    if helm list -n kube-system | grep -q aws-load-balancer-controller; then
        log "Uninstalling aws-load-balancer-controller Helm release..."
        helm uninstall aws-load-balancer-controller -n kube-system
        
        # Wait for controller pods to be deleted
        log "Waiting for controller pods to terminate..."
        kubectl wait --for=delete pod -l app.kubernetes.io/name=aws-load-balancer-controller \
            -n kube-system --timeout=60s 2>/dev/null || true
        
        success "AWS Load Balancer Controller uninstalled"
    else
        warning "AWS Load Balancer Controller Helm release not found"
    fi
    
    # Clean up any remaining resources
    log "Cleaning up controller resources..."
    kubectl delete deployment aws-load-balancer-controller -n kube-system 2>/dev/null || true
    kubectl delete service aws-load-balancer-webhook-service -n kube-system 2>/dev/null || true
    kubectl delete mutatingwebhookconfiguration aws-load-balancer-webhook 2>/dev/null || true
    kubectl delete validatingwebhookconfiguration aws-load-balancer-webhook 2>/dev/null || true
    
    success "ALB Controller resources cleaned up"
    echo ""
else
    log "⏭️  Skipping AWS Load Balancer Controller uninstallation"
    echo ""
fi

# Step 2: Delete Kubernetes Ingress resources and ALB
if [ "$SKIP_INGRESS" = false ]; then
    log "🌐 Deleting Kubernetes Ingress resources..."
    
    cd "$TERRAFORM_DIR"
    
    # Check if ingress resources exist
    INGRESS_CREATED=$(terraform output -raw onerecord_ingress_name 2>/dev/null || echo "not-created")
    
    if [ "$INGRESS_CREATED" != "not-created" ]; then
        log "Destroying ingress resources via Terraform..."
        log "  - This will trigger ALB deletion automatically"
        
        # Disable ingress creation in Terraform
        if [ "$SKIP_DNS" = false ]; then
            # If also destroying DNS, disable both
            terraform apply -var="create_ingress=false" -var="create_dns=false" -auto-approve
        else
            # Only disable ingress
            terraform apply -var="create_ingress=false" -auto-approve
        fi
        
        # Wait for ALB to be deleted
        log "Waiting for ALB to be deleted (this may take 2-3 minutes)..."
        sleep 30
        
        for i in {1..12}; do
            ALB_COUNT=$(aws elbv2 describe-load-balancers --region "$AWS_REGION" \
                --query 'LoadBalancers[?contains(DNSName, `ontomeshalb`)].LoadBalancerArn' \
                --output text 2>/dev/null | wc -l)
            
            if [ "$ALB_COUNT" -eq 0 ]; then
                success "ALB has been deleted"
                break
            fi
            
            echo "ALB still exists, waiting... (attempt $i/12)"
            sleep 15
        done
        
        success "Ingress resources deleted"
    else
        warning "No ingress resources found in Terraform state"
    fi
    
    # Clean up any remaining ingress resources manually
    log "Cleaning up any remaining ingress resources..."
    kubectl delete ingress --all -n ontomesh 2>/dev/null || true
    kubectl delete ingress --all -n keycloak 2>/dev/null || true
    kubectl delete ingress --all -n graphdb 2>/dev/null || true
    
    success "Ingress cleanup complete"
    echo ""
else
    log "⏭️  Skipping Ingress resource deletion"
    echo ""
fi

# Step 3: Delete Route53 DNS and ACM Certificate
if [ "$SKIP_DNS" = false ]; then
    log "🌍 Deleting Route53 DNS and ACM Certificate..."
    
    cd "$TERRAFORM_DIR"
    
    # Check if DNS was created
    HOSTED_ZONE=$(terraform output -raw hosted_zone_id 2>/dev/null || echo "not-created")
    
    if [ "$HOSTED_ZONE" != "not-created" ] && [ -n "$HOSTED_ZONE" ]; then
        log "DNS resources found, destroying via Terraform..."
        
        # This will delete:
        # - Route53 hosted zone
        # - DNS records (A records for subdomains)
        # - ACM certificate
        terraform apply -var="create_dns=false" -var="create_ingress=false" -auto-approve
        
        success "Route53 and ACM resources deleted"
    else
        warning "No Route53 hosted zone found in Terraform state"
    fi
    
    echo ""
else
    log "⏭️  Skipping DNS and SSL certificate deletion"
    echo ""
fi

# Step 4: Delete IAM Roles and Policies
if [ "$KEEP_IAM_ROLES" = false ]; then
    log "🔐 Deleting IAM roles and policies..."
    
    cd "$TERRAFORM_DIR"
    
    # Check current state
    ALB_ROLE_ARN=$(terraform output -raw alb_controller_role_arn 2>/dev/null || echo "")
    
    if [ -n "$ALB_ROLE_ARN" ] && [ "$ALB_ROLE_ARN" != "not-created" ]; then
        warning "IAM roles exist but won't be deleted with this command"
        warning "IAM roles are managed by main Terraform and require full infrastructure teardown"
        warning "To delete IAM roles, run: cd $TERRAFORM_DIR && terraform destroy"
        echo ""
        warning "Alternatively, manually delete:"
        echo "  - IAM Role: ontomesh-alb-controller-role"
        echo "  - IAM Policy: ontomesh-alb-controller-policy"
        echo "  - IAM Policy: ontomesh-alb-controller-additional-policy"
    else
        log "No ALB Controller IAM roles found"
    fi
    
    echo ""
else
    log "⏭️  Keeping IAM roles (as requested)"
    echo ""
fi

# Step 5: Clean up ServiceAccount
log "🧹 Cleaning up ALB Controller ServiceAccount..."
kubectl delete serviceaccount aws-load-balancer-controller -n kube-system 2>/dev/null || true
success "ServiceAccount cleanup complete"
echo ""

log "✅ DNS/SSL/Load Balancer uninstallation complete!"
echo ""
echo "=========================================="
echo "         UNINSTALLATION SUMMARY          "
echo "=========================================="

if [ "$SKIP_ALB_CONTROLLER" = false ]; then
    echo "✅ AWS Load Balancer Controller: Uninstalled"
else
    echo "⏭️  AWS Load Balancer Controller: Skipped"
fi

if [ "$SKIP_INGRESS" = false ]; then
    echo "✅ Ingress Resources: Deleted"
    echo "✅ Application Load Balancer: Deleted"
else
    echo "⏭️  Ingress Resources: Skipped"
fi

if [ "$SKIP_DNS" = false ]; then
    echo "✅ Route53 Hosted Zone: Deleted"
    echo "✅ DNS Records: Deleted"
    echo "✅ ACM Certificate: Deleted"
else
    echo "⏭️  DNS and SSL: Skipped"
fi

if [ "$KEEP_IAM_ROLES" = true ]; then
    echo "⚠️  IAM Roles: Preserved (use Terraform destroy for full cleanup)"
else
    echo "⚠️  IAM Roles: Require manual deletion or Terraform destroy"
fi

echo ""
echo "🔍 Verification:"
echo "   # Check Helm releases:"
echo "   helm list -n kube-system"
echo ""
echo "   # Check ingress resources:"
echo "   kubectl get ingress -A"
echo ""
echo "   # Check ALBs in AWS:"
echo "   aws elbv2 describe-load-balancers --region $AWS_REGION"
echo ""
echo "   # Check Route53 hosted zones:"
echo "   aws route53 list-hosted-zones"
echo ""
echo "   # Check ACM certificates:"
echo "   aws acm list-certificates --region $AWS_REGION"
echo ""
echo "=========================================="
echo ""
log "ℹ️  Note: If you want to completely remove all infrastructure,"
log "   run: cd $TERRAFORM_DIR && terraform destroy"
echo ""
