#####################################################
# AUTOMATED SSH KEY GENERATION AND AWS KEYPAIR SETUP
#####################################################

# 1️⃣ Generate a new RSA private key
resource "tls_private_key" "generated" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# 2️⃣ Store the private key locally (secure file)
resource "local_file" "private_key" {
  content              = tls_private_key.generated.private_key_pem
  filename             = "${path.module}/ontomesh-ssh-key.pem"
  file_permission      = "0600"
  directory_permission = "0700"
}

# 3️⃣ Create AWS key pair using the generated public key
resource "aws_key_pair" "this" {
  key_name   = "${var.project_name}-keypair"
  public_key = tls_private_key.generated.public_key_openssh

  tags = merge(var.tags, {
    Name = "${var.project_name}-keypair"
  })
}

# 4️⃣ Output path to the private key file (for SSH)
output "private_key_path" {
  value       = local_file.private_key.filename
  description = "Path to the locally saved SSH private key"
}

output "ssh_key_name" {
  value       = aws_key_pair.this.key_name
  description = "AWS EC2 Key Pair name"
}

