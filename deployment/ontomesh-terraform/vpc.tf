module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.5.0"

  name = "${var.project_name}-vpc"
  cidr = var.vpc_cidr

  azs             = ["ap-south-2a", "ap-south-2b"]
  public_subnets  = var.public_subnets
  private_subnets = var.private_subnets

  enable_nat_gateway = true
  single_nat_gateway = true

  # Tags for AWS Load Balancer Controller subnet discovery
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }

  tags = var.tags
}

