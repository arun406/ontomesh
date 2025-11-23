###############################################################################
# ACM Certificate for *.ontomesh.com
# Note: Set create_dns=true to enable ACM resources after apps are deployed
###############################################################################

# Create wildcard SSL/TLS certificate
resource "aws_acm_certificate" "ontomesh_wildcard" {
  count = var.create_dns ? 1 : 0
  
  domain_name       = "*.${var.domain_name}"
  validation_method = "DNS"

  subject_alternative_names = [
    var.domain_name  # Also include the apex domain
  ]

  lifecycle {
    create_before_destroy = true
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-wildcard-cert"
    }
  )
}

# Create Route53 records for DNS validation
resource "aws_route53_record" "cert_validation" {
  for_each = var.create_dns ? {
    for dvo in aws_acm_certificate.ontomesh_wildcard[0].domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  } : {}

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = aws_route53_zone.ontomesh[0].zone_id
}

# Skip certificate validation - it will validate automatically in the background
# resource "aws_acm_certificate_validation" "ontomesh_wildcard" {
#   certificate_arn         = aws_acm_certificate.ontomesh_wildcard.arn
#   validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
#
#   timeouts {
#     create = "10m"
#   }
# }

###############################################################################
# Outputs
###############################################################################

output "acm_certificate_arn" {
  description = "ARN of the ACM certificate for *.ontomesh.com"
  value       = var.create_dns ? aws_acm_certificate.ontomesh_wildcard[0].arn : "not-created"
}

output "acm_certificate_status" {
  description = "Status of the ACM certificate"
  value       = var.create_dns ? aws_acm_certificate.ontomesh_wildcard[0].status : "not-created"
}

output "acm_certificate_domain" {
  description = "Domain name of the ACM certificate"
  value       = var.create_dns ? aws_acm_certificate.ontomesh_wildcard[0].domain_name : "not-created"
}

# Removed output that depended on validation resource
# output "acm_certificate_validated" {
#   description = "Whether the certificate has been validated"
#   value       = aws_acm_certificate_validation.ontomesh_wildcard.id != "" ? "true" : "false"
# }
