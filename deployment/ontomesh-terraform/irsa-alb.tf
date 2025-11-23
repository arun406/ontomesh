###############################################################################
# AWS Load Balancer Controller IRSA
# Note: Set create_ingress=true to create these resources after apps are deployed
###############################################################################

# Data source to get the AWS Load Balancer Controller IAM policy
data "http" "alb_controller_iam_policy" {
  count = var.create_ingress ? 1 : 0
  url   = "https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.11.0/docs/install/iam_policy.json"
}

# Create IAM policy for AWS Load Balancer Controller
resource "aws_iam_policy" "alb_controller" {
  count       = var.create_ingress ? 1 : 0
  name        = "${var.project_name}-alb-controller-policy"
  description = "IAM policy for AWS Load Balancer Controller"
  policy      = data.http.alb_controller_iam_policy[0].response_body

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-alb-controller-policy"
    }
  )
}

# IAM role for AWS Load Balancer Controller
resource "aws_iam_role" "alb_controller" {
  count = var.create_ingress ? 1 : 0
  name  = "${var.project_name}-alb-controller-role"

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
            "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:sub" = "system:serviceaccount:${var.alb_controller_namespace}:${var.alb_controller_service_account_name}"
            "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:aud" = "sts.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-alb-controller-role"
    }
  )
}

# Attach the policy to the role
resource "aws_iam_role_policy_attachment" "alb_controller_attach" {
  count      = var.create_ingress ? 1 : 0
  policy_arn = aws_iam_policy.alb_controller[0].arn
  role       = aws_iam_role.alb_controller[0].name
}

# Additional IAM policy for missing permissions (SetRulePriorities, ModifyRule)
resource "aws_iam_policy" "alb_controller_additional" {
  count       = var.create_ingress ? 1 : 0
  name        = "${var.project_name}-alb-controller-additional-policy"
  description = "Additional IAM permissions for AWS Load Balancer Controller"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "elasticloadbalancing:SetRulePriorities",
          "elasticloadbalancing:ModifyRule"
        ]
        Resource = "*"
      }
    ]
  })

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-alb-controller-additional-policy"
    }
  )
}

# Attach the additional policy to the role
resource "aws_iam_role_policy_attachment" "alb_controller_additional_attach" {
  count      = var.create_ingress ? 1 : 0
  policy_arn = aws_iam_policy.alb_controller_additional[0].arn
  role       = aws_iam_role.alb_controller[0].name
}

# NOTE: ServiceAccount is NOT created by Terraform
# The Helm chart (aws-load-balancer-controller) will create the ServiceAccount
# and annotate it with the IAM role ARN
# This avoids conflicts between Terraform and Helm trying to manage the same resource

###############################################################################
# Outputs
###############################################################################

output "alb_controller_role_arn" {
  description = "ARN of the IAM role used by AWS Load Balancer Controller"
  value       = var.create_ingress ? aws_iam_role.alb_controller[0].arn : "not-created"
}

output "alb_controller_role_name" {
  description = "Name of the IAM role used by AWS Load Balancer Controller"
  value       = var.create_ingress ? aws_iam_role.alb_controller[0].name : "not-created"
}

output "alb_controller_service_account_name" {
  description = "Name of the Kubernetes ServiceAccount for AWS Load Balancer Controller (managed by Helm)"
  value       = var.alb_controller_service_account_name
}

output "alb_controller_namespace" {
  description = "Namespace where AWS Load Balancer Controller is deployed"
  value       = var.alb_controller_namespace
}
