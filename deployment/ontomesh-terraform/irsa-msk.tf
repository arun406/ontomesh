########################################
# IRSA Setup for MSK Client
########################################

# Fetch EKS cluster info
data "aws_eks_cluster" "this" {
  name       = module.eks.cluster_name
  depends_on = [module.eks]
}

data "aws_eks_cluster_auth" "this" {
  name       = module.eks.cluster_name
  depends_on = [module.eks]
}

# ########################################
# # Kubernetes Provider
# ########################################
# provider "kubernetes" {
#   host                   = data.aws_eks_cluster.this.endpoint
#   cluster_ca_certificate = base64decode(data.aws_eks_cluster.this.certificate_authority[0].data)
#   token                  = data.aws_eks_cluster_auth.this.token
# }

########################################
# Create namespace for MSK clients
########################################
resource "kubernetes_namespace" "msk" {
  metadata {
    name = var.msk_namespace
  }
}

########################################
# IAM Role for MSK Client Pods (IRSA)
########################################
resource "aws_iam_role" "msk_client_role" {
  name = "${var.project_name}-msk-client-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = module.eks.oidc_provider_arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${replace(module.eks.oidc_provider, "https://", "")}:sub" = "system:serviceaccount:${var.msk_namespace}:${var.msk_service_account_name}"
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "msk_client_policy_attach" {
  role       = aws_iam_role.msk_client_role.name
  policy_arn = aws_iam_policy.msk_policy.arn
}

########################################
# Kubernetes Service Account (IRSA Binding)
########################################
resource "kubernetes_service_account" "msk_client_sa" {
  metadata {
    name      = var.msk_service_account_name
    namespace = kubernetes_namespace.msk.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.msk_client_role.arn
    }
  }

  depends_on = [
    module.eks,
    kubernetes_namespace.msk
  ]
}

########################################
# Outputs
########################################
output "msk_client_role_arn" {
  value = aws_iam_role.msk_client_role.arn
}

output "msk_service_account_name" {
  value = kubernetes_service_account.msk_client_sa.metadata[0].name
}
