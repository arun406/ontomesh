output "vpc_id" {
  value = module.vpc.vpc_id
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "eks_cluster_arn" {
  description = "ARN of the EKS cluster"
  value       = module.eks.cluster_arn
}

output "eks_cluster_endpoint" {
  description = "Kubernetes API server endpoint for the EKS cluster"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_certificate_authority_data" {
  description = "Base64 encoded certificate authority data for the cluster"
  value       = module.eks.cluster_certificate_authority_data
}

output "eks_cluster_oidc_issuer_url" {
  description = "OIDC issuer URL for the cluster (full URL)"
  value       = module.eks.cluster_oidc_issuer_url
}

output "eks_oidc_provider_arn" {
  description = "ARN of the IAM OIDC provider created for the cluster (if enable_irsa=true)"
  value       = try(module.eks.oidc_provider_arn, "")
}

output "eks_cluster_version" {
  description = "Kubernetes version of the EKS cluster"
  value       = module.eks.cluster_version
}

output "eks_cluster_status" {
  description = "Status of the EKS cluster"
  value       = module.eks.cluster_status
}

output "eks_node_iam_role_arn" {
  description = "IAM role ARN used by EKS nodes (if created by the module)"
  value       = try(module.eks.node_iam_role_arn, "")
}

output "eks_node_security_group_id" {
  description = "Security group ID used by EKS nodes (shared node SG)"
  value       = try(module.eks.node_security_group_id, "")
}

output "eks_managed_node_groups" {
  description = "Map of managed node group attributes created by the module"
  value       = try(module.eks.eks_managed_node_groups, {})
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}

output "rds_endpoint_with_port" {
  description = "RDS endpoint address with port"
  value       = "${aws_db_instance.this.address}:${aws_db_instance.this.port}"
}

output "rds_port" {
  description = "RDS port"
  value       = aws_db_instance.this.port
}

output "rds_identifier" {
  description = "RDS instance identifier (resource id)"
  value       = aws_db_instance.this.id
}

output "rds_arn" {
  description = "RDS instance ARN"
  value       = aws_db_instance.this.arn
}

output "rds_db_name" {
  description = "Database name"
  value       = var.rds_db_name
}

output "rds_username" {
  description = "Database master username (from variables)"
  value       = var.rds_username
}

output "rds_instance_class" {
  description = "RDS instance class"
  value       = var.rds_instance_class
}

# output "msk_bootstrap_brokers_tls" {
#   description = "TLS bootstrap broker string for MSK cluster"
#   value       = module.msk.bootstrap_brokers_tls
# }

output "msk_arn" {
  description = "ARN of the MSK cluster"
  value       = module.msk.arn
}

# EBS CSI IRSA outputs
output "ebs_csi_role_arn" {
  description = "ARN of the IAM role used by the EBS CSI driver (IRSA)"
  value       = try(aws_iam_role.ebs_csi.arn, "")
}

output "ebs_csi_role_name" {
  description = "Name of the IAM role used by the EBS CSI driver"
  value       = try(aws_iam_role.ebs_csi.name, "")
}

output "ebs_csi_role_id" {
  description = "ID of the IAM role used by the EBS CSI driver"
  value       = try(aws_iam_role.ebs_csi.id, "")
}

output "ebs_csi_policy_attachment_id" {
  description = "IAM role policy attachment id for the EBS CSI driver"
  value       = try(aws_iam_role_policy_attachment.ebs_csi_attach.id, "")
}

output "ebs_csi_policy_attachment_policy_arn" {
  description = "Policy ARN attached to the EBS CSI IAM role"
  value       = try(aws_iam_role_policy_attachment.ebs_csi_attach.policy_arn, "")
}

output "ebs_csi_service_account_name" {
  description = "Kubernetes ServiceAccount name for the EBS CSI driver"
  value       = try(kubernetes_service_account.ebs_csi_sa.metadata[0].name, "")
}

output "ebs_csi_service_account_namespace" {
  description = "Kubernetes ServiceAccount namespace for the EBS CSI driver"
  value       = try(kubernetes_service_account.ebs_csi_sa.metadata[0].namespace, "")
}

output "ebs_csi_service_account_role_annotation" {
  description = "Value of the eks.amazonaws.com/role-arn annotation on the EBS CSI ServiceAccount"
  value       = try(kubernetes_service_account.ebs_csi_sa.metadata[0].annotations["eks.amazonaws.com/role-arn"], "")
}

# MSK IRSA related outputs (additional to the ones already declared in irsa-msk.tf)
output "msk_client_role_name" {
  description = "Name of the IAM role used by MSK client pods"
  value       = try(aws_iam_role.msk_client_role.name, "")
}

output "msk_client_role_id" {
  description = "ID of the IAM role used by MSK client pods"
  value       = try(aws_iam_role.msk_client_role.id, "")
}

output "msk_service_account_namespace" {
  description = "Namespace of the MSK client ServiceAccount"
  value       = try(kubernetes_service_account.msk_client_sa.metadata[0].namespace, "")
}

output "msk_service_account_role_annotation" {
  description = "Role ARN annotation on the MSK client ServiceAccount"
  value       = try(kubernetes_service_account.msk_client_sa.metadata[0].annotations["eks.amazonaws.com/role-arn"], "")
}

