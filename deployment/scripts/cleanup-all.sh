#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# ONTOMESH CLEANUP SCRIPT
# 
# This script performs complete cleanup of the Ontomesh deployment:
# - Removes all Kubernetes resources
# - Uninstalls Helm charts
# - Destroys Terraform infrastructure
# - Cleans up AWS resources
#
# WARNING: This will destroy ALL resources. Use with caution!
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
FORCE=${FORCE:-false}
SKIP_TERRAFORM=${SKIP_TERRAFORM:-false}
SKIP_K8S=${SKIP_K8S:-false}

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
${BLUE}Ontomesh Cleanup Script${NC}

${RED}WARNING: This will destroy ALL Ontomesh resources!${NC}

${YELLOW}USAGE:${NC}
    $0 [OPTIONS]

${YELLOW}OPTIONS:${NC}
    --force                   Skip confirmation prompts
    --skip-terraform          Skip Terraform destruction
    --skip-k8s               Skip Kubernetes cleanup
    --aws-region REGION       AWS region (default: ap-south-2)
    --help                    Show this help

${YELLOW}CLEANUP PHASES:${NC}
    1. Remove Kubernetes Applications
    2. Uninstall Helm Charts
    3. Delete Kubernetes Resources
    4. Destroy Terraform Infrastructure
    5. Clean up AWS Resources

${YELLOW}EXAMPLES:${NC}
    # Interactive cleanup (recommended):
    $0

    # Force cleanup without prompts:
    $0 --force

    # Cleanup only Kubernetes resources:
    $0 --skip-terraform

    # Cleanup only Terraform resources:
    $0 --skip-k8s

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --force)
            FORCE=true
            shift
            ;;
        --skip-terraform)
            SKIP_TERRAFORM=true
            shift
            ;;
        --skip-k8s)
            SKIP_K8S=true
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

# Confirmation prompt
confirm_cleanup() {
    if [ "$FORCE" = true ]; then
        warning "Force mode enabled, skipping confirmation"
        return 0
    fi
    
    echo ""
    echo "${RED}⚠️  DANGER ZONE ⚠️${NC}"
    echo ""
    echo "This will completely destroy the Ontomesh deployment including:"
    echo "  • All Kubernetes applications and data"
    echo "  • EKS cluster and worker nodes"
    echo "  • RDS database with all data"
    echo "  • MSK Kafka cluster"
    echo "  • VPC and networking resources"
    echo "  • Route53 DNS records"
    echo "  • SSL certificates"
    echo ""
    echo "${RED}This action cannot be undone!${NC}"
    echo ""
    
    read -p "Are you sure you want to proceed? (type 'yes' to confirm): " confirmation
    
    if [ "$confirmation" != "yes" ]; then
        log "Cleanup cancelled by user"
        exit 0
    fi
    
    echo ""
    read -p "Final confirmation - type 'DESTROY' to proceed: " final_confirmation
    
    if [ "$final_confirmation" != "DESTROY" ]; then
        log "Cleanup cancelled by user"
        exit 0
    fi
}

# Clean up Kubernetes resources
cleanup_kubernetes() {
    if [ "$SKIP_K8S" = true ]; then
        log "⏭️  Skipping Kubernetes cleanup"
        return 0
    fi
    
    log "🧹 Cleaning up Kubernetes resources..."
    
    # Check if kubectl is configured
    if ! kubectl cluster-info >/dev/null 2>&1; then
        warning "kubectl not configured or cluster not accessible"
        return 0
    fi
    
    # Delete applications
    log "Removing OneRecord applications..."
    kubectl delete deployment onerecord-server -n ontomesh --ignore-not-found=true
    kubectl delete service onerecord-server-service -n ontomesh --ignore-not-found=true
    
    log "Removing GraphDB..."
    kubectl delete deployment graphdb -n graphdb --ignore-not-found=true
    kubectl delete service graphdb-service -n graphdb --ignore-not-found=true
    kubectl delete pvc graphdb-pvc -n graphdb --ignore-not-found=true
    
    log "Removing Keycloak..."
    kubectl delete deployment keycloak -n keycloak --ignore-not-found=true
    kubectl delete service keycloak-service -n keycloak --ignore-not-found=true
    kubectl delete pvc keycloak-pvc -n keycloak --ignore-not-found=true
    
    # Delete ingress resources
    log "Removing ingress resources..."
    kubectl delete ingress --all -A --ignore-not-found=true
    
    # Wait for ALB cleanup
    log "Waiting for ALB cleanup..."
    sleep 30
    
    # Delete namespaces
    log "Removing namespaces..."
    kubectl delete namespace ontomesh --ignore-not-found=true
    kubectl delete namespace graphdb --ignore-not-found=true
    kubectl delete namespace keycloak --ignore-not-found=true
    
    success "Kubernetes resources cleaned up"
}

# Uninstall Helm charts
cleanup_helm() {
    if [ "$SKIP_K8S" = true ]; then
        log "⏭️  Skipping Helm cleanup"
        return 0
    fi
    
    log "📦 Uninstalling Helm charts..."
    
    # Check if kubectl is configured
    if ! kubectl cluster-info >/dev/null 2>&1; then
        warning "kubectl not configured, skipping Helm cleanup"
        return 0
    fi
    
    # Uninstall AWS Load Balancer Controller
    if helm list -n kube-system | grep -q aws-load-balancer-controller; then
        log "Uninstalling AWS Load Balancer Controller..."
        helm uninstall aws-load-balancer-controller -n kube-system
        success "AWS Load Balancer Controller uninstalled"
    else
        warning "AWS Load Balancer Controller not found"
    fi
    
    # Uninstall EBS CSI Driver
    if helm list -n kube-system | grep -q aws-ebs-csi-driver; then
        log "Uninstalling EBS CSI Driver..."
        helm uninstall aws-ebs-csi-driver -n kube-system
        success "EBS CSI Driver uninstalled"
    else
        warning "EBS CSI Driver not found"
    fi
    
    # Wait for cleanup
    log "Waiting for Helm resources to be cleaned up..."
    sleep 30
}

# Destroy Terraform infrastructure
destroy_terraform() {
    if [ "$SKIP_TERRAFORM" = true ]; then
        log "⏭️  Skipping Terraform destruction"
        return 0
    fi
    
    log "💥 Destroying Terraform infrastructure..."
    
    cd "$TERRAFORM_DIR"
    
    # Initialize Terraform
    log "Initializing Terraform..."
    terraform init
    
    # Plan destruction
    log "Planning Terraform destruction..."
    terraform plan -destroy -var="aws_region=$AWS_REGION"
    
    # Destroy infrastructure
    log "Destroying Terraform infrastructure..."
    terraform destroy -var="aws_region=$AWS_REGION" -auto-approve
    
    success "Terraform infrastructure destroyed"
}

# Clean up additional AWS resources
cleanup_aws_resources() {
    log "🧽 Cleaning up additional AWS resources..."
    
    # Clean up any remaining load balancers
    log "Checking for remaining load balancers..."
    local lbs=$(aws elbv2 describe-load-balancers --region "$AWS_REGION" --query 'LoadBalancers[?contains(LoadBalancerName, `k8s-ontomesh`)].LoadBalancerArn' --output text || true)
    
    if [ -n "$lbs" ]; then
        warning "Found remaining load balancers, attempting cleanup..."
        for lb in $lbs; do
            aws elbv2 delete-load-balancer --load-balancer-arn "$lb" --region "$AWS_REGION" || true
        done
    fi
    
    # Clean up security groups (with retry)
    log "Cleaning up security groups..."
    local sgs=$(aws ec2 describe-security-groups --region "$AWS_REGION" --query 'SecurityGroups[?contains(Description, `ontomesh`) || contains(GroupName, `ontomesh`)].GroupId' --output text || true)
    
    if [ -n "$sgs" ]; then
        warning "Found Ontomesh security groups, attempting cleanup..."
        for sg in $sgs; do
            aws ec2 delete-security-group --group-id "$sg" --region "$AWS_REGION" || true
        done
    fi
    
    success "AWS resource cleanup completed"
}

# Verify cleanup
verify_cleanup() {
    log "🔍 Verifying cleanup..."
    
    echo ""
    echo "=== Cleanup Verification ==="
    
    # Check Kubernetes resources
    if ! [ "$SKIP_K8S" = true ] && kubectl cluster-info >/dev/null 2>&1; then
        echo "Remaining Kubernetes resources:"
        kubectl get all -A | grep -E "(keycloak|graphdb|ontomesh)" || echo "✓ No application resources found"
        echo ""
        kubectl get ingress -A || echo "✓ No ingress resources found"
    fi
    
    # Check Terraform state
    if ! [ "$SKIP_TERRAFORM" = true ] && [ -f "$TERRAFORM_DIR/terraform.tfstate" ]; then
        cd "$TERRAFORM_DIR"
        local resources=$(terraform state list 2>/dev/null | wc -l)
        if [ "$resources" -gt 0 ]; then
            warning "$resources Terraform resources remain"
        else
            success "No Terraform resources remain"
        fi
    fi
    
    # Check AWS resources
    echo "Checking AWS resources..."
    aws eks describe-cluster --name ontomesh-eks --region "$AWS_REGION" >/dev/null 2>&1 && warning "EKS cluster still exists" || success "EKS cluster destroyed"
    aws rds describe-db-instances --db-instance-identifier ontomesh-rds --region "$AWS_REGION" >/dev/null 2>&1 && warning "RDS instance still exists" || success "RDS instance destroyed"
    aws kafka describe-cluster --cluster-arn "*" --region "$AWS_REGION" 2>/dev/null | grep -q ontomesh && warning "MSK cluster still exists" || success "MSK cluster destroyed"
}

# Main function
main() {
    echo ""
    echo "🧹 ONTOMESH CLEANUP ORCHESTRATOR"
    echo "================================="
    echo "AWS Region: $AWS_REGION"
    echo "Force mode: $FORCE"
    echo "Skip Terraform: $SKIP_TERRAFORM"
    echo "Skip Kubernetes: $SKIP_K8S"
    echo "================================="
    echo ""
    
    # Get confirmation
    confirm_cleanup
    
    log "🚀 Starting cleanup process..."
    
    # Phase 1: Clean up Kubernetes resources
    cleanup_kubernetes
    
    # Phase 2: Uninstall Helm charts
    cleanup_helm
    
    # Phase 3: Destroy Terraform infrastructure
    destroy_terraform
    
    # Phase 4: Clean up additional AWS resources
    cleanup_aws_resources
    
    # Phase 5: Verify cleanup
    verify_cleanup
    
    success "🎉 Cleanup completed successfully!"
    echo ""
    echo "Summary:"
    echo "  ✅ Kubernetes applications removed"
    echo "  ✅ Helm charts uninstalled"
    echo "  ✅ Terraform infrastructure destroyed"
    echo "  ✅ AWS resources cleaned up"
    echo ""
    echo "The Ontomesh deployment has been completely removed."
    echo ""
    echo "Note: Some AWS resources (like Route53 hosted zones) may"
    echo "have been retained for safety. Check the AWS console if needed."
}

# Run main function
main "$@"