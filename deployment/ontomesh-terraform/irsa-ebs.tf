###############################################################
# IRSA for EBS CSI Driver
###############################################################

locals {
  # Remove https:// from OIDC issuer URL
  oidc_issuer_without_https = replace(module.eks.cluster_oidc_issuer_url, "https://", "")
}

resource "aws_iam_role" "ebs_csi" {
  name = "${var.project_name}-ebs-csi-sa-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "sts:AssumeRoleWithWebIdentity"
        Principal = {
          Federated = module.eks.oidc_provider_arn
        }
        Condition = {
          StringEquals = {
            # OIDC claim must end with :sub
            "${local.oidc_issuer_without_https}:sub" = "system:serviceaccount:kube-system:ebs-csi-controller-sa"
          }
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name = "${var.project_name}-ebs-csi-sa-role"
  })
}

resource "aws_iam_role_policy_attachment" "ebs_csi_attach" {
  role       = aws_iam_role.ebs_csi.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}

resource "kubernetes_service_account" "ebs_csi_sa" {
  metadata {
    name      = "ebs-csi-controller-sa"
    namespace = "kube-system"
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.ebs_csi.arn
    }
  }

  depends_on = [
    module.eks,
    aws_iam_role.ebs_csi
  ]
}
