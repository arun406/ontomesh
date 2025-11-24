###############################################################################
# Kubernetes Ingress Resources
# Note: Set create_ingress=true to create ingress resources after apps are deployed
###############################################################################

# OneRecord Server Ingress
resource "kubernetes_ingress_v1" "onerecord" {
  count = var.create_ingress ? 1 : 0
  
  metadata {
    name      = "onerecord-ingress"
    namespace = var.msk_namespace
    annotations = merge(
      {
        "alb.ingress.kubernetes.io/scheme"           = "internet-facing"
        "alb.ingress.kubernetes.io/target-type"      = "ip"
        "alb.ingress.kubernetes.io/group.name"       = var.ingress_group_name
        "alb.ingress.kubernetes.io/healthcheck-path" = "/actuator/health"
      },
      var.create_dns ? {
        "alb.ingress.kubernetes.io/listen-ports"     = jsonencode([{ HTTP = 80 }, { HTTPS = 443 }])
        "alb.ingress.kubernetes.io/ssl-redirect"     = "443"
        "alb.ingress.kubernetes.io/certificate-arn"  = aws_acm_certificate.ontomesh_wildcard[0].arn
      } : {
        "alb.ingress.kubernetes.io/listen-ports"     = jsonencode([{ HTTP = 80 }])
      }
    )
  }

  spec {
    ingress_class_name = "alb"

    rule {
      host = "airline.${var.domain_name}"

      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = "onerecord-server-service"
              port {
                number = 8080
              }
            }
          }
        }
      }
    }
  }
  
  lifecycle {
    create_before_destroy = false
  }
}

# Keycloak Ingress
resource "kubernetes_ingress_v1" "keycloak" {
  count = var.create_ingress ? 1 : 0
  
  metadata {
    name      = "keycloak-ingress"
    namespace = "keycloak"
    annotations = merge(
      {
        "alb.ingress.kubernetes.io/scheme"                      = "internet-facing"
        "alb.ingress.kubernetes.io/target-type"                 = "ip"
        "alb.ingress.kubernetes.io/group.name"                  = var.ingress_group_name
        "alb.ingress.kubernetes.io/healthcheck-path"            = "/"
        "alb.ingress.kubernetes.io/success-codes"               = "200-399"
        "alb.ingress.kubernetes.io/backend-protocol"            = "HTTP"
      },
      var.create_dns ? {
        "alb.ingress.kubernetes.io/listen-ports"                = jsonencode([{ HTTP = 80 }, { HTTPS = 443 }])
        "alb.ingress.kubernetes.io/ssl-redirect"                = "443"
        "alb.ingress.kubernetes.io/certificate-arn"             = aws_acm_certificate.ontomesh_wildcard[0].arn
      } : {
        "alb.ingress.kubernetes.io/listen-ports"                = jsonencode([{ HTTP = 80 }])
      }
    )
  }

  spec {
    ingress_class_name = "alb"

    rule {
      host = "keycloak.${var.domain_name}"

      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = "keycloak"
              port {
                number = 8080
              }
            }
          }
        }
      }
    }
  }
  
  lifecycle {
    create_before_destroy = false
  }
}

# GraphDB Ingress
resource "kubernetes_ingress_v1" "graphdb" {
  count = var.create_ingress ? 1 : 0
  
  metadata {
    name      = "graphdb-ingress"
    namespace = "graphdb"
    annotations = merge(
      {
        "alb.ingress.kubernetes.io/scheme"           = "internet-facing"
        "alb.ingress.kubernetes.io/target-type"      = "ip"
        "alb.ingress.kubernetes.io/group.name"       = var.ingress_group_name
        "alb.ingress.kubernetes.io/healthcheck-path" = "/rest/repositories"
        "alb.ingress.kubernetes.io/backend-protocol" = "HTTP"
      },
      var.create_dns ? {
        "alb.ingress.kubernetes.io/listen-ports"     = jsonencode([{ HTTP = 80 }, { HTTPS = 443 }])
        "alb.ingress.kubernetes.io/ssl-redirect"     = "443"
        "alb.ingress.kubernetes.io/certificate-arn"  = aws_acm_certificate.ontomesh_wildcard[0].arn
      } : {
        "alb.ingress.kubernetes.io/listen-ports"     = jsonencode([{ HTTP = 80 }])
      }
    )
  }

  spec {
    ingress_class_name = "alb"

    rule {
      host = "graphdb.${var.domain_name}"

      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = "graphdb"
              port {
                number = 7200
              }
            }
          }
        }
      }
    }
  }
  
  lifecycle {
    create_before_destroy = false
  }
}

###############################################################################
# Outputs
###############################################################################

output "onerecord_ingress_name" {
  description = "Name of the OneRecord ingress resource"
  value       = var.create_ingress ? kubernetes_ingress_v1.onerecord[0].metadata[0].name : "not-created"
}

output "keycloak_ingress_name" {
  description = "Name of the Keycloak ingress resource"
  value       = var.create_ingress ? kubernetes_ingress_v1.keycloak[0].metadata[0].name : "not-created"
}

output "graphdb_ingress_name" {
  description = "Name of the GraphDB ingress resource"
  value       = var.create_ingress ? kubernetes_ingress_v1.graphdb[0].metadata[0].name : "not-created"
}

output "alb_dns_name" {
  description = "DNS name of the ALB (once created)"
  value       = var.create_ingress ? try(kubernetes_ingress_v1.onerecord[0].status[0].load_balancer[0].ingress[0].hostname, "pending") : "not-created"
}
