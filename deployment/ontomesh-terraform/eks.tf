########################################
# EKS Cluster
########################################

locals {
  caller_arn     = data.aws_caller_identity.current.arn
  caller_is_role = can(regex("role/", local.caller_arn))

  eks_map_roles = local.caller_is_role ? [
    {
      rolearn  = local.caller_arn
      username = "terraform-admin"
      groups   = ["system:masters"]
    }
  ] : []

  eks_map_users = local.caller_is_role ? [] : [
    {
      userarn  = local.caller_arn
      username = "terraform-admin"
      groups   = ["system:masters"]
    }
  ]
}

########################################
# EKS Module
########################################

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 21.0"

  name               = "${var.project_name}-eks"
  kubernetes_version = var.eks_version

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  ########################################
  # Enable All Core Add-ons
  ########################################
  addons = {
    coredns = {}
    kube-proxy = {}
    vpc-cni = { before_compute = true }
    eks-pod-identity-agent = { before_compute = true }

  }

  ########################################
  # Cluster Access
  ########################################

  endpoint_public_access = true
  enable_irsa            = true

  eks_managed_node_groups = {
    default = {
      instance_types = [var.eks_node_instance_type]
      desired_size   = var.eks_node_desired_capacity
      min_size       = var.eks_node_min_size
      max_size       = var.eks_node_max_size

      key_name = var.ssh_key_name

      additional_policies = {
        MSKAccess = aws_iam_policy.msk_policy.arn
      }

      tags = {
        Name = "${var.project_name}-eks-node"
      }
    }
  }

  ########################################
  # Secure EKS Access Entry
  ########################################

  access_entries = merge(
    try(var.access_entries, {}),
    {
      terraform_admin = {
        principal_arn = data.aws_caller_identity.current.arn
        policy_associations = {
          admin = {
            policy_arn = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
            access_scope = {
              type = "cluster"
            }
          }
        }
      }
    }
  )

  ########################################
  # Tags
  ########################################
  tags = merge(
    var.tags,
    {
      "kubernetes.io/cluster/${var.project_name}-eks" = "owned"
      "Environment"                                   = var.tags.Environment
    }
  )
}
