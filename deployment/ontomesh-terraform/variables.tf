###############################################
# GENERAL
###############################################
variable "project_name" {
  description = "Project name prefix for resources"
  type        = string
  default     = "ontomesh"
}

variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "ap-south-2"
}

variable "tags" {
  description = "Common tags applied to all resources"
  type        = map(string)
  default = {
    Environment = "dev"
    ManagedBy   = "Terraform"
  }
}

###############################################
# NETWORK
###############################################
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnets" {
  description = "List of public subnet CIDRs"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnets" {
  description = "List of private subnet CIDRs"
  type        = list(string)
  default     = ["10.0.3.0/24", "10.0.4.0/24"]
}

###############################################
# SSH KEYPAIR
###############################################
variable "ssh_key_name" {
  description = "EC2 Key Pair name"
  type        = string
  default     = "OneRecord-Key-Pair"
}

variable "admin_cidr" {
  description = "CIDR block allowed to SSH into nodes (e.g., your IP)"
  type        = string
}

###############################################
# EKS CONFIG
###############################################
variable "eks_version" {
  description = "EKS Kubernetes version"
  type        = string
  default     = "1.34"
}

variable "eks_node_instance_type" {
  description = "EKS worker node instance type"
  type        = string
  default     = "t3.large"
}

variable "eks_node_min_size" {
  type    = number
  default = 1
}

variable "eks_node_desired_capacity" {
  type    = number
  default = 1
}

variable "eks_node_max_size" {
  type    = number
  default = 3
}

###############################################
# MSK CONFIG
###############################################
variable "msk_broker_instance_type" {
  type    = string
  default = "kafka.t3.small"
}

variable "msk_num_brokers" {
  type    = number
  default = 2
}

variable "ebs_volume_size" {
  description = "EBS volume size (in GB) for each MSK broker"
  type        = number
  default     = 5

}

variable "map_roles" {
  description = "Additional EKS aws-auth mapRoles entries"
  type        = list(any)
  default     = []
}

variable "map_users" {
  description = "Additional EKS aws-auth mapUsers entries"
  type        = list(any)
  default     = []
}

variable "access_entries" {
  description = "Map of access entries to add to the cluster (module-specific)."
  type        = map(any)
  default     = {}
}
###############################################
# RDS CONFIG
###############################################
variable "rds_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "rds_db_name" {
  type    = string
  default = "keycloakdb"
}

variable "rds_username" {
  type    = string
  default = "ontomesh"
}

variable "rds_password" {
  type    = string
  default = "ChangeMe123!"
}

variable "msk_namespace" {
  description = "Kubernetes namespace where MSK client service account will be created"
  type        = string
  default     = "ontomesh"
}

variable "msk_service_account_name" {
  description = "Name of the Kubernetes service account for MSK client"
  type        = string
  default     = "ontomesh-kafka-client-sa"
}

###############################################
# ALB CONTROLLER CONFIG
###############################################
variable "alb_controller_namespace" {
  description = "Kubernetes namespace for AWS Load Balancer Controller"
  type        = string
  default     = "kube-system"
}

variable "alb_controller_service_account_name" {
  description = "Service account name for AWS Load Balancer Controller"
  type        = string
  default     = "aws-load-balancer-controller"
}

###############################################
# ROUTE53 & DOMAIN CONFIG
###############################################
variable "domain_name" {
  description = "Root domain name for Route53 hosted zone"
  type        = string
  default     = "ontomesh.com"
}

variable "ingress_group_name" {
  description = "ALB Ingress group name for sharing ALB across ingresses"
  type        = string
  default     = "ontomesh-alb"
}

variable "create_ingress" {
  description = "Whether to create Kubernetes ingress resources (set true after apps are deployed)"
  type        = bool
  default     = false
}

variable "create_dns" {
  description = "Whether to create Route53 DNS and ACM certificate (set true with create_ingress)"
  type        = bool
  default     = false
}

