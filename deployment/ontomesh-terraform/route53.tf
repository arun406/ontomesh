###############################################################################
# Route53 Configuration
# Note: Set create_dns=true and create_ingress=true to enable DNS after apps are deployed
###############################################################################

# Create the hosted zone for ontomesh.com
resource "aws_route53_zone" "ontomesh" {
  count = var.create_dns ? 1 : 0
  
  name          = var.domain_name
  comment       = "Hosted zone for ${var.domain_name} - managed by Terraform"
  force_destroy = true  # Allow destruction even if contains records

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-hosted-zone"
    }
  )
}

# DNS Record for OneRecord Server (airline subdomain)
# Note: Only create this after ingress has an ALB hostname
resource "aws_route53_record" "onerecord" {
  count = var.create_dns && var.create_ingress && try(kubernetes_ingress_v1.onerecord[0].status[0].load_balancer[0].ingress[0].hostname, "") != "" ? 1 : 0
  
  zone_id = aws_route53_zone.ontomesh[0].zone_id
  name    = "airline.${var.domain_name}"
  type    = "A"

  alias {
    name                   = kubernetes_ingress_v1.onerecord[0].status[0].load_balancer[0].ingress[0].hostname
    zone_id                = data.aws_elb_hosted_zone_id.main.id
    evaluate_target_health = true
  }

  depends_on = [kubernetes_ingress_v1.onerecord]
}

# DNS Record for Keycloak
resource "aws_route53_record" "keycloak" {
  count = var.create_dns && var.create_ingress && try(kubernetes_ingress_v1.keycloak[0].status[0].load_balancer[0].ingress[0].hostname, "") != "" ? 1 : 0
  
  zone_id = aws_route53_zone.ontomesh[0].zone_id
  name    = "keycloak.${var.domain_name}"
  type    = "A"

  alias {
    name                   = kubernetes_ingress_v1.keycloak[0].status[0].load_balancer[0].ingress[0].hostname
    zone_id                = data.aws_elb_hosted_zone_id.main.id
    evaluate_target_health = true
  }

  depends_on = [kubernetes_ingress_v1.keycloak]
}

# DNS Record for GraphDB
resource "aws_route53_record" "graphdb" {
  count = var.create_dns && var.create_ingress && try(kubernetes_ingress_v1.graphdb[0].status[0].load_balancer[0].ingress[0].hostname, "") != "" ? 1 : 0
  
  zone_id = aws_route53_zone.ontomesh[0].zone_id
  name    = "graphdb.${var.domain_name}"
  type    = "A"

  alias {
    name                   = kubernetes_ingress_v1.graphdb[0].status[0].load_balancer[0].ingress[0].hostname
    zone_id                = data.aws_elb_hosted_zone_id.main.id
    evaluate_target_health = true
  }

  depends_on = [kubernetes_ingress_v1.graphdb]
}

# Data source for ALB hosted zone ID (for ap-south-2 region)
data "aws_elb_hosted_zone_id" "main" {}

###############################################################################
# Outputs
###############################################################################

output "hosted_zone_id" {
  description = "Route53 hosted zone ID for ontomesh.com"
  value       = var.create_dns ? aws_route53_zone.ontomesh[0].zone_id : "not-created"
}

output "hosted_zone_name_servers" {
  description = "Name servers for the hosted zone"
  value       = var.create_dns ? aws_route53_zone.ontomesh[0].name_servers : []
}

output "onerecord_domain" {
  description = "OneRecord server domain name"
  value       = var.create_dns && length(aws_route53_record.onerecord) > 0 ? aws_route53_record.onerecord[0].fqdn : "not-created-yet"
}

output "keycloak_domain" {
  description = "Keycloak domain name"
  value       = var.create_dns && length(aws_route53_record.keycloak) > 0 ? aws_route53_record.keycloak[0].fqdn : "not-created-yet"
}

output "graphdb_domain" {
  description = "GraphDB domain name"
  value       = var.create_dns && length(aws_route53_record.graphdb) > 0 ? aws_route53_record.graphdb[0].fqdn : "not-created-yet"
}
