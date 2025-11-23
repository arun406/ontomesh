terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 6.20.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.37.1"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.1.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5.3"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7.2"
    }
    http = {
      source  = "hashicorp/http"
      version = "~> 3.4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

provider "kubernetes" {
  # host                   = module.eks.cluster_endpoint
  # cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)
  # token                  = data.aws_eks_cluster_auth.this.token
  host                   = data.aws_eks_cluster.this.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.this.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.this.token

}

