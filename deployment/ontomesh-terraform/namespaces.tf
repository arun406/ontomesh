###############################################################################
# Kubernetes Namespace Resources
###############################################################################

# Keycloak namespace
resource "kubernetes_namespace" "keycloak" {
  metadata {
    name = "keycloak"
    labels = {
      name                                 = "keycloak"
      "app.kubernetes.io/managed-by"       = "terraform"
      "app.kubernetes.io/part-of"          = "ontomesh"
    }
  }

  depends_on = [module.eks]
}

# GraphDB namespace
resource "kubernetes_namespace" "graphdb" {
  metadata {
    name = "graphdb"
    labels = {
      name                                 = "graphdb"
      "app.kubernetes.io/managed-by"       = "terraform"
      "app.kubernetes.io/part-of"          = "ontomesh"
    }
  }

  depends_on = [module.eks]
}

###############################################################################
# Outputs
###############################################################################

output "keycloak_namespace" {
  description = "Name of the Keycloak namespace"
  value       = kubernetes_namespace.keycloak.metadata[0].name
}

output "graphdb_namespace" {
  description = "Name of the GraphDB namespace"
  value       = kubernetes_namespace.graphdb.metadata[0].name
}