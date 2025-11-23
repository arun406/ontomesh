# Ontomesh AWS Installation Guide

Complete installation guide for deploying Ontomesh platform on AWS EKS with three-phase architecture.

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture](#architecture)
4. [Quick Start](#quick-start)
5. [Detailed Installation](#detailed-installation)
6. [Configuration Options](#configuration-options)
7. [Accessing Services](#accessing-services)
8. [Troubleshooting](#troubleshooting)
9. [Cleanup](#cleanup)
10. [Cost Considerations](#cost-considerations)

---

## Overview

Ontomesh is a ONE Record API implementation deployed on AWS EKS with:
- **Infrastructure**: EKS cluster, VPC, MSK Kafka, RDS PostgreSQL
- **Applications**: Keycloak (IAM), GraphDB (RDF store), OneRecord API
- **Public Access**: Application Load Balancer, Route53 DNS, ACM SSL certificates

### Deployment Philosophy

The deployment uses a **three-phase architecture** to avoid circular dependencies:

1. **Phase 1: Core Infrastructure** - EKS, VPC, MSK, RDS (no ingress, no DNS)
2. **Phase 2: Applications** - Keycloak, GraphDB, OneRecord (ClusterIP only)
3. **Phase 3: Public Access** - ALB Controller, Ingress, Route53, ACM SSL

This allows independent management of infrastructure, applications, and public access layers.

---

## Prerequisites

### Required Tools

Install the following tools before beginning:

```bash
# AWS CLI (v2.x)
aws --version

# Terraform (>= 1.3.0)
terraform version

# kubectl (>= 1.27)
kubectl version --client

# Helm (>= 3.12)
helm version

# jq (for JSON parsing)
jq --version
```

### AWS Account Setup

1. **AWS Account** with administrator access
2. **AWS Region**: `ap-south-2` (Hyderabad) or your preferred region
3. **Route53 Domain**: Register `ontomesh.com` or use your own domain
4. **AWS Credentials** configured:

```bash
aws configure
# AWS Access Key ID: [your-key-id]
# AWS Secret Access Key: [your-secret-key]
# Default region name: ap-south-2
# Default output format: json
```

5. **IAM Permissions** required:
   - VPC, Subnet, Route Table management
   - EKS cluster and node group management
   - MSK cluster management
   - RDS instance management
   - IAM role and policy management
   - Route53 hosted zone and record management
   - ACM certificate management
   - ELB/ALB management

### Local Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/ontomesh.git
cd ontomesh

# Set AWS region
export AWS_REGION=ap-south-2

# Configure kubectl for EKS (after Phase 1)
aws eks update-kubeconfig --name ontomesh-eks --region $AWS_REGION
```

---

## Architecture

### Phase 1: Core Infrastructure

**Components:**
- VPC with public/private subnets
- EKS cluster (`ontomesh-eks`)
- MSK Kafka cluster (2 brokers)
- RDS PostgreSQL database
- EBS CSI Driver for persistent volumes
- Security groups and IAM roles

**NOT included:**
- ALB Controller
- Ingress resources
- Route53 DNS records
- SSL certificates

### Phase 2: Applications

**Components:**
- **Keycloak** (port 8080, 1Gi storage) - Identity and access management
- **GraphDB** (port 7200, 5Gi storage) - RDF triple store
- **OneRecord API** (port 8080) - ONE Record server

**Service Type:** ClusterIP (internal cluster access only)

**NOT included:**
- External load balancer
- Public DNS records
- HTTPS access

### Phase 3: Public Access

**Components:**
- AWS Load Balancer Controller (Helm chart v2.11.0)
- Kubernetes Ingress resources
- Application Load Balancer (auto-provisioned)
- Route53 hosted zone for `ontomesh.com`
- ACM wildcard certificate (`*.ontomesh.com`)
- A records for services

**Access URLs:**
- `https://airline.ontomesh.com` - OneRecord API
- `https://keycloak.ontomesh.com` - Keycloak admin console
- `https://graphdb.ontomesh.com` - GraphDB workbench

### Deployment Flow

```
Phase 1: Infrastructure (20-25 min)
    ↓
Phase 2: Applications (5-10 min)
    ↓
Phase 3: Public Access (10-15 min)
    ↓
Total: 40-50 minutes
```

---

## Quick Start

### One-Command Deployment

Deploy all three phases automatically:

```bash
cd deployment/scripts

# Deploy infrastructure and applications
./deploy-all.sh --aws-region ap-south-2

# After applications are running, enable public access
./post-deployment-dns-ssl.sh
```

### Verify Deployment

```bash
# Check EKS nodes
kubectl get nodes

# Check all pods
kubectl get pods -A

# Check services
kubectl get svc -A

# Check ingress
kubectl get ingress -A

# Test HTTPS access
curl -I https://graphdb.ontomesh.com
```

---

## Detailed Installation

### Phase 1: Infrastructure Deployment

Deploy core infrastructure (EKS, VPC, MSK, RDS):

```bash
cd deployment/scripts
./deploy-infrastructure.sh --aws-region ap-south-2
```

**What gets created:**
- ✅ VPC with CIDR 10.0.0.0/16
- ✅ 3 public subnets (10.0.1.0/24, 10.0.2.0/24, 10.0.3.0/24)
- ✅ 3 private subnets (10.0.11.0/24, 10.0.12.0/24, 10.0.13.0/24)
- ✅ Internet Gateway and NAT Gateways
- ✅ EKS cluster (Kubernetes 1.30)
- ✅ EKS node group (t3.medium, 1-3 nodes)
- ✅ MSK cluster (kafka.t3.small, 2 brokers)
- ✅ RDS PostgreSQL (db.t3.micro, 20GB)
- ✅ EBS CSI Driver with IRSA

**Deployment time:** ~20-25 minutes

**Verify:**
```bash
# Configure kubectl
aws eks update-kubeconfig --name ontomesh-eks --region ap-south-2

# Check nodes
kubectl get nodes
# Expected: 1-3 nodes in Ready state

# Check EBS CSI Driver
kubectl get pods -n kube-system | grep ebs-csi
# Expected: ebs-csi-controller and ebs-csi-node pods running

# Verify no ingress yet
kubectl get ingress -A
# Expected: No resources found
```

### Phase 2: Application Deployment

Deploy applications with ClusterIP services:

```bash
cd deployment/scripts
./deploy-applications.sh --aws-region ap-south-2
```

**What gets created:**
- ✅ Namespaces: `keycloak`, `graphdb`, `ontomesh`
- ✅ Keycloak pod with ClusterIP service (port 8080)
- ✅ GraphDB pod with ClusterIP service (port 7200)
- ✅ OneRecord API pod with ClusterIP service (port 8080)
- ✅ PersistentVolumeClaims (Keycloak 1Gi, GraphDB 5Gi)
- ✅ Keycloak initialized with `ontomesh` realm
- ✅ GraphDB initialized with `ontomesh` repository

**Deployment time:** ~5-10 minutes

**Verify:**
```bash
# Check pods
kubectl get pods -A | grep -E "(keycloak|graphdb|onerecord)"
# Expected: All pods Running with READY 1/1

# Check services (all ClusterIP)
kubectl get svc -A | grep -E "(keycloak|graphdb|onerecord)"
# Expected: All services showing ClusterIP type

# Check PVCs
kubectl get pvc -A
# Expected: keycloak-pvc (1Gi Bound), graphdb-pvc (5Gi Bound)

# Verify no external access
kubectl get ingress -A
# Expected: No resources found

aws elbv2 describe-load-balancers --region ap-south-2 | grep k8s-ontomesh
# Expected: No results (no ALB yet)
```

### Phase 3: Public Access Setup

Enable HTTPS access via ALB, Route53, and ACM:

```bash
cd deployment/scripts
./post-deployment-dns-ssl.sh
```

**What happens:**

**Step 1: Verify Applications**
- Checks that all pods are Running
- Verifies PVCs are bound
- Confirms services exist

**Step 2: Create ALB Controller Resources** (Terraform)
```bash
terraform apply -var="create_ingress=true"
```
- ✅ IAM policy for ALB Controller
- ✅ IAM role with OIDC trust relationship
- ✅ Kubernetes Service Account with IAM annotation
- ✅ Ingress resources for OneRecord, Keycloak, GraphDB

**Step 3: Install ALB Controller** (Helm)
```bash
helm install aws-load-balancer-controller eks/aws-load-balancer-controller
```
- ✅ Controller deployment in `kube-system` namespace
- ✅ Watches for Ingress resources
- ✅ Provisions ALB automatically

**Step 4: Wait for ALB Provisioning**
- Monitors ingress for ALB hostname (~2-3 minutes)
- Verifies target groups are created
- Checks target health

**Step 5: Enable DNS and SSL** (Terraform)
```bash
terraform apply -var="create_ingress=true" -var="create_dns=true"
```
- ✅ Route53 hosted zone for `ontomesh.com`
- ✅ ACM wildcard certificate (`*.ontomesh.com`)
- ✅ DNS validation records
- ✅ A records pointing to ALB

**Step 6: Wait for Certificate Validation**
- ACM validates via DNS (~5-10 minutes)
- Certificate status: PENDING_VALIDATION → ISSUED

**Deployment time:** ~10-15 minutes

**Verify:**
```bash
# Check ingress resources
kubectl get ingress -A
# Expected: 3 ingresses with ALB hostname

# Check ALB
aws elbv2 describe-load-balancers --region ap-south-2 | grep k8s-ontomesh
# Expected: 1 load balancer

# Check DNS resolution
dig airline.ontomesh.com +short
dig keycloak.ontomesh.com +short
dig graphdb.ontomesh.com +short
# Expected: ALB IP addresses

# Check certificate status
aws acm list-certificates --region ap-south-2 | grep ontomesh
# Expected: Certificate ARN

# Test HTTPS access
curl -I https://graphdb.ontomesh.com
# Expected: HTTP/2 200 or 406 (valid SSL)
```

---

## Configuration Options

### Infrastructure Options

Edit `deployment/ontomesh-terraform/terraform.tfvars`:

```hcl
# Region
aws_region = "ap-south-2"

# EKS Configuration
cluster_name = "ontomesh-eks"
cluster_version = "1.30"

# Node Group
node_instance_types = ["t3.medium"]
node_desired_size = 1
node_min_size = 1
node_max_size = 3

# MSK Configuration
kafka_instance_type = "kafka.t3.small"
kafka_broker_count = 2

# RDS Configuration
rds_instance_class = "db.t3.micro"
rds_allocated_storage = 20
rds_database_name = "keycloakdb"
rds_username = "keycloakuser"

# VPC Configuration
vpc_cidr = "10.0.0.0/16"
```

### Application Options

#### Keycloak Configuration

```bash
# Deploy with custom service type
./deploy-keycloak.sh \
  --namespace keycloak \
  --service-type ClusterIP \
  --storage-class gp2 \
  --pvc-size 1Gi
```

Default credentials:
- **Username:** `admin`
- **Password:** `admin` (change in production!)

#### GraphDB Configuration

```bash
# Deploy with custom storage
./deploy-graphdb.sh \
  --namespace graphdb \
  --service-type ClusterIP \
  --storage-class gp2 \
  --pvc-size 5Gi
```

#### OneRecord Configuration

OneRecord server is configured via environment variables:
- `SPRING_PROFILES_ACTIVE=k8s`
- `GRAPHDB_URL=http://graphdb-service.graphdb.svc.cluster.local:7200`
- `KEYCLOAK_URL=http://keycloak-service.keycloak.svc.cluster.local:8080`

### DNS and SSL Options

#### Custom Domain

Edit `deployment/ontomesh-terraform/variables.tf`:

```hcl
variable "domain_name" {
  default = "yourdomain.com"  # Change this
}
```

Then update ingress hostnames in `ingress.tf`:
```hcl
# OneRecord
host = "airline.yourdomain.com"

# Keycloak
host = "keycloak.yourdomain.com"

# GraphDB
host = "graphdb.yourdomain.com"
```

#### HTTP-Only Deployment

To deploy without HTTPS (for testing):

```bash
# Skip DNS and SSL
cd deployment/scripts
./deploy-all.sh --skip-dns

# Applications accessible via ALB DNS name only
kubectl get ingress -n graphdb graphdb-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

### Selective Deployment

#### Infrastructure Only

```bash
./deploy-all.sh --skip-apps
```

#### Applications Only (if infrastructure exists)

```bash
./deploy-all.sh --skip-infra
```

#### Skip DNS Setup

```bash
./deploy-all.sh --skip-dns
# Manual Phase 3 later:
./post-deployment-dns-ssl.sh
```

---

## Accessing Services

### After Phase 3 Completion

#### OneRecord API

**URL:** https://airline.ontomesh.com

**Health Check:**
```bash
curl https://airline.ontomesh.com/actuator/health
```

**Get Access Token:**
```bash
TOKEN=$(curl -X POST https://keycloak.ontomesh.com/realms/ontomesh/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=external-one-record-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" | jq -r '.access_token')
```

**Create Logistics Object:**
```bash
curl -X POST https://airline.ontomesh.com/logistics-objects \
  -H "Content-Type: application/ld+json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "@context": {
      "cargo": "https://onerecord.iata.org/ns/cargo#"
    },
    "@type": "cargo:Piece",
    "cargo:goodsDescription": "Test shipment"
  }'
```

#### Keycloak Admin Console

**URL:** https://keycloak.ontomesh.com

**Default Credentials:**
- Username: `admin`
- Password: `admin`

**Access Realms:**
- Navigate to "Realms" dropdown
- Select "ontomesh" realm
- Manage clients, users, roles

#### GraphDB Workbench

**URL:** https://graphdb.ontomesh.com

**Access Repository:**
- Repository: `ontomesh`
- Default credentials: None (configure as needed)

**SPARQL Query:**
```bash
curl -X POST https://graphdb.ontomesh.com/repositories/ontomesh \
  -H "Content-Type: application/sparql-query" \
  -H "Accept: application/sparql-results+json" \
  -d "SELECT * WHERE { ?s ?p ?o } LIMIT 10"
```

### Internal Access (Within Cluster)

If Phase 3 is not deployed, access services via port-forward:

#### Keycloak Port Forward
```bash
kubectl port-forward -n keycloak svc/keycloak-service 8080:8080
# Access at: http://localhost:8080
```

#### GraphDB Port Forward
```bash
kubectl port-forward -n graphdb svc/graphdb-service 7200:7200
# Access at: http://localhost:7200
```

#### OneRecord Port Forward
```bash
kubectl port-forward -n ontomesh svc/onerecord-server-service 8080:8080
# Access at: http://localhost:8080
```

---

## Troubleshooting

### Phase 1 Issues

#### EKS Cluster Not Accessible

```bash
# Update kubeconfig
aws eks update-kubeconfig --name ontomesh-eks --region ap-south-2

# Verify connection
kubectl get nodes

# If connection fails, check IAM permissions
aws sts get-caller-identity
```

#### RDS Connection Failure

```bash
# Check RDS endpoint
aws rds describe-db-instances \
  --db-instance-identifier ontomesh-rds \
  --region ap-south-2 \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

# Verify security group allows EKS access
```

#### EBS CSI Driver Not Working

```bash
# Check driver pods
kubectl get pods -n kube-system | grep ebs-csi

# Check driver logs
kubectl logs -n kube-system deployment/ebs-csi-controller

# Verify IRSA role
kubectl describe sa -n kube-system ebs-csi-controller-sa
```

### Phase 2 Issues

#### Pods Not Running

```bash
# Check pod status
kubectl get pods -A

# Describe problematic pod
kubectl describe pod <pod-name> -n <namespace>

# Check pod logs
kubectl logs <pod-name> -n <namespace>

# Common issues:
# - Image pull errors: Check image availability
# - CrashLoopBackOff: Check application logs
# - Pending: Check PVC binding or node resources
```

#### PVC Stuck in Pending

```bash
# Check PVC status
kubectl get pvc -A

# Describe PVC
kubectl describe pvc <pvc-name> -n <namespace>

# Check storage class
kubectl get storageclass

# Verify EBS CSI Driver is running
kubectl get pods -n kube-system | grep ebs-csi

# Check events
kubectl get events -n <namespace> --sort-by='.lastTimestamp'
```

#### Keycloak Initialization Failed

```bash
# Check Keycloak logs
kubectl logs -n keycloak deployment/keycloak

# Verify RDS connection
kubectl exec -n keycloak deployment/keycloak -- \
  psql -h <rds-endpoint> -U keycloakuser -d keycloakdb -c '\l'

# Re-run initialization
cd deployment/scripts
./init-keycloak.sh --namespace keycloak
```

#### GraphDB Repository Not Created

```bash
# Check GraphDB logs
kubectl logs -n graphdb deployment/graphdb

# Verify GraphDB is accessible
kubectl exec -n graphdb deployment/graphdb -- \
  curl -s http://localhost:7200/rest/repositories

# Re-run initialization
cd deployment/scripts
./init-graphdb.sh --namespace graphdb --repo-id ontomesh
```

### Phase 3 Issues

#### ALB Not Provisioned

```bash
# Check ALB Controller logs
kubectl logs -n kube-system deployment/aws-load-balancer-controller

# Check ingress events
kubectl describe ingress -n graphdb graphdb-ingress

# Verify IAM role exists
aws iam get-role --role-name ontomesh-alb-controller-role --region ap-south-2

# Check service account annotation
kubectl describe sa -n kube-system aws-load-balancer-controller

# Common issues:
# - Missing IAM permissions
# - Service account not annotated
# - Ingress class mismatch
```

#### Target Groups Using Wrong Port

```bash
# List target groups
aws elbv2 describe-target-groups --region ap-south-2

# If ports are wrong, delete ingresses and recreate
kubectl delete ingress -A --all

cd deployment/ontomesh-terraform
terraform apply -var="create_ingress=true"
```

#### Certificate Stuck in PENDING_VALIDATION

```bash
# Check certificate status
aws acm describe-certificate \
  --certificate-arn $(terraform output -raw acm_certificate_arn) \
  --region ap-south-2 \
  --query 'Certificate.Status' \
  --output text

# Check DNS validation records
aws route53 list-resource-record-sets \
  --hosted-zone-id $(terraform output -raw hosted_zone_id) \
  | grep _acm-challenge

# If validation fails, destroy and recreate
cd deployment/ontomesh-terraform
terraform destroy -target=aws_acm_certificate.ontomesh_wildcard[0]
terraform apply -var="create_ingress=true" -var="create_dns=true"

# Wait 5-10 minutes for validation
```

#### DNS Not Resolving

```bash
# Get nameservers
terraform output hosted_zone_name_servers

# Verify nameservers at domain registrar
# (must match Route53 nameservers)

# Wait for propagation (5-30 minutes)

# Test from different DNS servers
dig @8.8.8.8 graphdb.ontomesh.com +short
dig @1.1.1.1 graphdb.ontomesh.com +short
nslookup graphdb.ontomesh.com 8.8.8.8

# Check Route53 records
aws route53 list-resource-record-sets \
  --hosted-zone-id $(terraform output -raw hosted_zone_id)
```

#### HTTPS Connection Refused

```bash
# Check /etc/hosts for conflicts
grep -i ontomesh /etc/hosts
# Remove any localhost entries

# Flush DNS cache (macOS)
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder

# Flush DNS cache (Linux)
sudo systemd-resolve --flush-caches

# Check ALB listeners
aws elbv2 describe-listeners \
  --load-balancer-arn $(aws elbv2 describe-load-balancers \
    --query "LoadBalancers[?contains(LoadBalancerName, 'k8s-ontomesh')].LoadBalancerArn" \
    --output text --region ap-south-2) \
  --region ap-south-2

# Verify certificate is ISSUED
aws acm describe-certificate \
  --certificate-arn $(terraform output -raw acm_certificate_arn) \
  --region ap-south-2
```

#### Target Health Check Failing

```bash
# Check target health
ALB_ARN=$(aws elbv2 describe-load-balancers \
  --query "LoadBalancers[?contains(LoadBalancerName, 'k8s-ontomesh')].LoadBalancerArn" \
  --output text --region ap-south-2)

TG_ARNS=$(aws elbv2 describe-target-groups \
  --load-balancer-arn $ALB_ARN \
  --query "TargetGroups[].TargetGroupArn" \
  --output text --region ap-south-2)

for TG in $TG_ARNS; do
  echo "Target Group: $TG"
  aws elbv2 describe-target-health \
    --target-group-arn $TG \
    --region ap-south-2
done

# Check pod readiness
kubectl get pods -A -o wide

# Check service endpoints
kubectl get endpoints -A
```

### Common Solutions

#### Complete Redeployment

```bash
# Destroy everything
cd deployment/scripts
./cleanup-all.sh

# Wait for cleanup to complete (~10 minutes)

# Redeploy from scratch
./deploy-all.sh --aws-region ap-south-2
./post-deployment-dns-ssl.sh
```

#### Redeploy Applications Only

```bash
# Delete application namespaces
kubectl delete namespace ontomesh keycloak graphdb

# Wait for cleanup
kubectl get namespace

# Redeploy applications
cd deployment/scripts
./deploy-applications.sh
```

#### Toggle Public Access

```bash
# Disable public access
cd deployment/ontomesh-terraform
terraform apply -var="create_ingress=false" -var="create_dns=false"

# Re-enable public access
terraform apply -var="create_ingress=true" -var="create_dns=true"
```

---

## Cleanup

### Uninstall Applications Only

Preserve infrastructure but remove applications:

```bash
cd deployment/scripts
./uninstall-applications.sh --delete-namespaces --delete-pvcs
```

### Complete Cleanup

Remove all AWS resources:

```bash
cd deployment/scripts
./cleanup-all.sh
```

**Cleanup order:**
1. Delete Helm releases (ALB Controller, EBS CSI Driver)
2. Delete Kubernetes resources (ingress, apps, PVCs)
3. Wait for ALB deletion (~2-3 minutes)
4. Terraform destroy: DNS, ACM, Route53
5. Terraform destroy: Ingress resources
6. Terraform destroy: ALB Controller IRSA
7. Terraform destroy: EBS CSI IRSA, MSK IRSA
8. Terraform destroy: RDS, MSK, EKS
9. Terraform destroy: VPC, Subnets, Gateways

**Cleanup time:** ~10-15 minutes

### Manual Cleanup (if scripts fail)

```bash
# Delete Helm releases
helm uninstall aws-load-balancer-controller -n kube-system
helm uninstall aws-ebs-csi-driver -n kube-system
helm uninstall keycloak -n keycloak
helm uninstall graphdb -n graphdb

# Delete Kubernetes resources
kubectl delete namespace ontomesh keycloak graphdb
kubectl delete ingress -A --all

# Wait for ALB deletion
aws elbv2 describe-load-balancers --region ap-south-2 | grep k8s-ontomesh

# Terraform destroy
cd deployment/ontomesh-terraform
terraform destroy -auto-approve
```

---

## Cost Considerations

### Monthly Cost Estimate (ap-south-2)

| Service | Configuration | Monthly Cost (USD) |
|---------|---------------|-------------------|
| **EKS Control Plane** | 1 cluster | $73.00 |
| **EC2 (EKS Nodes)** | 1-3 x t3.medium | $25-75 |
| **MSK (Kafka)** | 2 x kafka.t3.small | $64.00 |
| **RDS (PostgreSQL)** | 1 x db.t3.micro | $15.00 |
| **EBS Volumes** | 20GB + PVCs (6Gi) | $3.00 |
| **NAT Gateway** | 1-3 gateways | $32-96 |
| **Application Load Balancer** | 1 ALB | $16-22 |
| **Route53 Hosted Zone** | 1 zone | $0.50 |
| **Route53 Queries** | 1M queries | $0.40 |
| **ACM Certificate** | 1 certificate | $0.00 (free) |
| **Data Transfer** | Varies | $5-20 |
| **Total** | | **~$233-375/month** |

### Cost Optimization Tips

1. **Use Spot Instances** for EKS nodes (save ~70%)
2. **Stop MSK during development** (save $64/month)
3. **Use single NAT Gateway** in dev (save ~$64/month)
4. **Skip Phase 3 in dev** (save $17/month on ALB)
5. **Use smaller RDS instance** (db.t3.micro is cheapest)
6. **Clean up unused EBS volumes** regularly
7. **Enable EKS cluster autoscaling** (scale to zero overnight)

### Development Setup (Minimal Cost)

```bash
# Minimal configuration
# - 1 t3.small node
# - 1 NAT Gateway
# - Skip Phase 3 (no ALB/DNS)
# - Stop MSK when not in use

# Estimated: ~$120-150/month
```

---

## Additional Resources

### Documentation

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [ONE Record API](https://github.com/IATA-Cargo/ONE-Record)

### Scripts Reference

| Script | Purpose | Phase |
|--------|---------|-------|
| `deploy-all.sh` | Complete deployment orchestrator | 1 & 2 |
| `deploy-infrastructure.sh` | Deploy AWS infrastructure | 1 |
| `deploy-applications.sh` | Deploy all applications | 2 |
| `deploy-keycloak.sh` | Deploy Keycloak only | 2 |
| `init-keycloak.sh` | Initialize Keycloak realm | 2 |
| `deploy-graphdb.sh` | Deploy GraphDB only | 2 |
| `init-graphdb.sh` | Initialize GraphDB repository | 2 |
| `post-deployment-dns-ssl.sh` | Setup DNS, SSL, ALB | 3 |
| `uninstall-applications.sh` | Uninstall apps only | - |
| `cleanup-all.sh` | Complete cleanup | - |

### Support

For issues or questions:
1. Check troubleshooting section above
2. Review logs: `kubectl logs <pod-name> -n <namespace>`
3. Check AWS console for resource status
4. Verify Terraform state: `terraform show`

---

## Summary

**Quick Commands:**

```bash
# Complete deployment
cd deployment/scripts
./deploy-all.sh
./post-deployment-dns-ssl.sh

# Verify
kubectl get pods -A
kubectl get ingress -A
curl https://graphdb.ontomesh.com

# Cleanup
./cleanup-all.sh
```

**Deployment Time:** ~40-50 minutes total
- Phase 1: 20-25 minutes
- Phase 2: 5-10 minutes
- Phase 3: 10-15 minutes

**Access:** After Phase 3 completion:
- OneRecord API: https://airline.ontomesh.com
- Keycloak: https://keycloak.ontomesh.com (admin/admin)
- GraphDB: https://graphdb.ontomesh.com

**Monthly Cost:** ~$233-375 (can be optimized to ~$120-150 for development)
