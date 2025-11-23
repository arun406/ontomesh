########################################
# MSK Cluster - IAM Authentication Only
########################################

module "msk" {
  source  = "terraform-aws-modules/msk-kafka-cluster/aws"
  version = "2.13.0"

  name                   = "${var.project_name}-msk"
  kafka_version          = "3.8.x"
  number_of_broker_nodes = var.msk_num_brokers

  broker_node_client_subnets  = module.vpc.private_subnets
  broker_node_instance_type   = var.msk_broker_instance_type
  broker_node_security_groups = [aws_security_group.msk_sg.id]

  broker_node_storage_info = {
    ebs_storage_info = {
      volume_size = var.ebs_volume_size
    }
  }

  ########################################
  # Security & Authentication
  ########################################
  encryption_in_transit_client_broker = "TLS"
  encryption_in_transit_in_cluster    = true

  # Enable both TLS and IAM authentication
  client_authentication = {
    unauthenticated = false
    sasl = {
      iam   = true
      scram = false
    }
  }

  create_scram_secret_association = false # No SCRAM secrets needed

  ########################################
  # Monitoring & Configuration
  ########################################
  enhanced_monitoring = "PER_TOPIC_PER_BROKER"

  configuration_server_properties = {
    "auto.create.topics.enable" = true
    "delete.topic.enable"       = true
  }

  jmx_exporter_enabled    = false
  node_exporter_enabled   = false
  cloudwatch_logs_enabled = false
  s3_logs_enabled         = false

  scaling_max_capacity = 512
  scaling_target_value = 80

  tags = merge(
    var.tags,
    {
      Name        = "${var.project_name}-msk"
      Terraform   = "true"
      Environment = var.tags.Environment
    }
  )
}

########################################
# MSK Security Group
########################################
resource "aws_security_group" "msk_sg" {
  name        = "${var.project_name}-msk-sg"
  description = "Security group for MSK cluster"
  vpc_id      = module.vpc.vpc_id

  # Allow inbound traffic from EKS nodes (MSK clients)
  ingress {
    description     = "Allow Kafka client connections from EKS nodes (IAM auth)"
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  ingress {
    description     = "Allow Kafka client connections from EKS nodes (TLS)"
    from_port       = 9094
    to_port         = 9094
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  # Allow inter-broker communication
  ingress {
    description = "Allow inter-broker communication"
    from_port   = 9092
    to_port     = 9094
    protocol    = "tcp"
    self        = true
  }

  # Outbound to anywhere (or restrict to admin_cidr)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [var.admin_cidr]
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.project_name}-msk-sg"
    }
  )
}

########################################
# Outputs
########################################
output "msk_cluster_name" {
  description = "Name of the MSK cluster"
  value       = module.msk.cluster_name
}

output "msk_security_group_id" {
  description = "Security group ID for MSK brokers"
  value       = aws_security_group.msk_sg.id
}

output "msk_bootstrap_brokers_tls" {
  description = "TLS bootstrap brokers for MSK clients"
  value       = module.msk.bootstrap_brokers_tls
}

output "msk_bootstrap_brokers_sasl_iam" {
  description = "IAM SASL bootstrap brokers for MSK clients"
  value       = module.msk.bootstrap_brokers_sasl_iam
}
