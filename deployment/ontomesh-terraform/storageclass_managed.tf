/*
  StorageClass configuration for EKS.
  
  The gp2 storage class is created by default in EKS clusters.
  We use a data source to reference it instead of trying to create it.
  
  The EBS CSI Driver is installed separately via Helm (see deploy-infrastructure.sh).
*/

variable "storage_class" {
  description = "Name of the storage class to use"
  type        = string
  default     = "gp2"
}

# Reference the existing gp2 storage class created by EKS
# This avoids the "already exists" error
data "kubernetes_storage_class_v1" "gp2" {
  metadata {
    name = "gp2"
  }
  
  depends_on = [module.eks]
}
