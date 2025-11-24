/*
  StorageClass configuration for EKS with EBS CSI Driver.
  
  Creates a gp2 storage class that uses the EBS CSI Driver provisioner (ebs.csi.aws.com).
  This replaces the deprecated kubernetes.io/aws-ebs provisioner.
  
  The EBS CSI Driver itself is installed via Helm (see deploy-infrastructure.sh).
*/

variable "storage_class" {
  description = "Name of the storage class to use"
  type        = string
  default     = "gp2"
}

# Create gp2 StorageClass with EBS CSI Driver provisioner
resource "kubernetes_storage_class_v1" "gp2" {
  metadata {
    name = "gp2"
    annotations = {
      "storageclass.kubernetes.io/is-default-class" = "true"
    }
  }

  storage_provisioner    = "ebs.csi.aws.com"
  reclaim_policy         = "Delete"
  volume_binding_mode    = "WaitForFirstConsumer"
  allow_volume_expansion = true

  parameters = {
    type      = "gp3"
    encrypted = "true"
  }

  depends_on = [
    module.eks,
    kubernetes_service_account.ebs_csi_sa
  ]
}
