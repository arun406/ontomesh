resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-rds-subnet"
  subnet_ids = module.vpc.private_subnets
  tags       = var.tags
}

resource "aws_db_instance" "this" {
  identifier             = "${var.project_name}-rds"
  engine                 = "postgres"
  engine_version         = "15"
  instance_class         = var.rds_instance_class
  db_name                = var.rds_db_name
  username               = var.rds_username
  password               = var.rds_password
  allocated_storage      = 10
  skip_final_snapshot    = true
  publicly_accessible    = false
  vpc_security_group_ids = [module.vpc.default_security_group_id]
  db_subnet_group_name   = aws_db_subnet_group.this.name

  tags = var.tags
}

