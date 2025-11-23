#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# ENABLE DNS AND SSL
# 
# This script:
# 1. Installs AWS Load Balancer Controller
# 2. Enables Route53 DNS and ACM SSL certificate creation
#
# Run this AFTER applications have been deployed successfully.
#
# Prerequisites:
# 1. Infrastructure deployed (EKS, VPC, MSK, RDS, EBS CSI)
# 2. Applications deployed (Keycloak, GraphDB, OneRecord)
###############################################################################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
TERRAFORM_DIR="$REPO_ROOT/deployment/ontomesh-terraform"
AWS_REGION=${AWS_REGION:-ap-south-2}

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

error() {
    echo -e "${RED}✗ $1${NC}"
    exit 1
}

log "🌐 Setting up DNS, SSL, and Load Balancer..."
echo "==========================================="
echo ""

# Step 1: Verify applications are running
log "Step 1: Verifying applications are deployed and running..."

REQUIRED_PODS=(
    "keycloak:keycloak"
    "graphdb:graphdb"
    "ontomesh:onerecord-server"
)

for pod_check in "${REQUIRED_PODS[@]}"; do
    IFS=':' read -r namespace app <<< "$pod_check"
    if ! kubectl get pods -n "$namespace" -l app="$app" 2>/dev/null | grep -q Running; then
        error "Application $app in namespace $namespace is not running. Deploy applications first!"
    fi
    success "$app is running in $namespace"
done

success "All applications are running"
echo ""

# Step 2: Create ALB Controller IAM role and ingress resources (Terraform)
log "Step 2: Creating ALB Controller IAM role and Kubernetes ingress resources..."
cd "$TERRAFORM_DIR"

log "Enabling ingress creation via Terraform..."
log "  - Creates ALB Controller IAM role and policies"
log "  - Creates Kubernetes ingress resources (onerecord, keycloak, graphdb)"
log "  - Note: ServiceAccount will be created by Helm in next step"
terraform apply -var="create_ingress=true" -auto-approve

success "IAM role and ingress resources created"
echo ""

# Step 3: Install AWS Load Balancer Controller via Helm
log "Step 3: Installing AWS Load Balancer Controller via Helm..."
log "  - Helm will create the ServiceAccount and annotate it with IAM role"
log "  - ALB Controller will watch for ingress resources and provision ALB"

# Get cluster details
CLUSTER_NAME=$(terraform output -raw cluster_name 2>/dev/null || echo "ontomesh-eks")
CLUSTER_VPC=$(terraform output -raw vpc_id 2>/dev/null)
ALB_ROLE_ARN=$(terraform output -raw alb_controller_role_arn 2>/dev/null)

if [ -z "$CLUSTER_VPC" ]; then
    error "Cannot get VPC ID from Terraform. Is infrastructure deployed?"
fi

if [ -z "$ALB_ROLE_ARN" ] || [ "$ALB_ROLE_ARN" = "not-created" ]; then
    error "ALB Controller IAM role not found. Terraform apply -var=\"create_ingress=true\" failed?"
fi

# Install AWS Load Balancer Controller via Helm
log "Adding EKS Helm repository..."
helm repo add eks https://aws.github.io/eks-charts 2>/dev/null || true
helm repo update

log "Installing/upgrading AWS Load Balancer Controller..."
log "Using IAM Role: $ALB_ROLE_ARN"
helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
    -n kube-system \
    --set clusterName="$CLUSTER_NAME" \
    --set serviceAccount.create=true \
    --set serviceAccount.name=aws-load-balancer-controller \
    --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"="$ALB_ROLE_ARN" \
    --set region="$AWS_REGION" \
    --set vpcId="$CLUSTER_VPC" \
    --wait

success "AWS Load Balancer Controller installed"

# Wait for controller to be ready
log "Waiting for ALB Controller to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/aws-load-balancer-controller -n kube-system

success "ALB Controller is ready"
echo ""

# Step 4: Wait for ALB provisioning
log "Step 4: Waiting for Application Load Balancer to be provisioned..."
ALB_DNS=""
for i in {1..30}; do
    ALB_DNS=$(kubectl get ingress -n graphdb graphdb-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
    if [ -n "$ALB_DNS" ]; then
        success "ALB provisioned: $ALB_DNS"
        break
    fi
    echo "Waiting for ALB hostname... attempt $i/30"
    sleep 10
done

if [ -z "$ALB_DNS" ]; then
    error "ALB hostname not available after 5 minutes. Check ingress status: kubectl get ingress -A"
fi

success "Application Load Balancer is ready"
echo ""

# Step 5: Enable DNS and SSL
log "Step 5: Enabling Route53 DNS and ACM SSL certificate..."
terraform apply -var="create_ingress=true" -var="create_dns=true" -auto-approve

log "Waiting for ACM certificate validation..."
CERT_ARN=$(terraform output -raw acm_certificate_arn 2>/dev/null || echo "")
if [ -n "$CERT_ARN" ]; then
    log "Certificate ARN: $CERT_ARN"
    log "Waiting for DNS validation (this may take 5-10 minutes)..."
    
    for i in {1..60}; do
        CERT_STATUS=$(aws acm describe-certificate --certificate-arn "$CERT_ARN" --region "$AWS_REGION" --query 'Certificate.Status' --output text 2>/dev/null || echo "UNKNOWN")
        
        if [ "$CERT_STATUS" = "ISSUED" ]; then
            success "Certificate validated and issued!"
            break
        elif [ "$CERT_STATUS" = "FAILED" ]; then
            error "Certificate validation failed. Check ACM console."
        else
            echo "Certificate status: $CERT_STATUS (attempt $i/60)"
            sleep 10
        fi
    done
    
    if [ "$CERT_STATUS" != "ISSUED" ]; then
        warning "Certificate validation still pending after 10 minutes. It may complete later."
    fi
else
    warning "Could not retrieve certificate ARN"
fi

success "DNS and SSL certificate enabled!"
echo ""

# Show DNS records
log "DNS Records Created:"
terraform output hosted_zone_name_servers
echo ""
echo "⚠️  IMPORTANT: Update your domain registrar with these name servers"
echo ""

success "🎉 DNS, SSL, and Load Balancer setup complete!"
echo ""
echo "=========================================="
echo "         CONFIGURATION COMPLETE          "
echo "=========================================="
echo ""
log "DNS Name Servers:"
terraform output hosted_zone_name_servers
echo ""
echo "⚠️  ACTION REQUIRED: Update your domain registrar"
echo "   Add the name servers shown above to ontomesh.com"
echo ""
echo "📝 Next steps:"
echo "   1. Update domain registrar with name servers (required)"
echo "   2. Wait for DNS propagation (5-30 minutes)"
echo "   3. SSL certificate will validate automatically"
echo "   4. Access applications:"
echo "      • https://airline.ontomesh.com"
echo "      • https://keycloak.ontomesh.com"
echo "      • https://graphdb.ontomesh.com"
echo ""
echo "🔍 Verification:"
echo "   - Check ALB: kubectl get ingress -A"
echo "   - Check DNS: dig airline.ontomesh.com"
echo "   - Check SSL: curl -I https://airline.ontomesh.com"
echo ""
echo "=========================================="
