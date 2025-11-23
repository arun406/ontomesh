########################################
# MSK Access IAM Policy
########################################

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "msk_policy" {
  statement {
    effect = "Allow"
    actions = [
      "kafka-cluster:Connect",
      "kafka-cluster:DescribeCluster"
    ]
    resources = [
      module.msk.arn
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "kafka-cluster:DescribeTopic",
      "kafka-cluster:CreateTopic",
      "kafka-cluster:WriteData",
      "kafka-cluster:ReadData"
    ]
    resources = [
      "arn:aws:kafka:${var.aws_region}:${data.aws_caller_identity.current.account_id}:topic/${module.msk.cluster_name}/*"
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "kafka-cluster:AlterGroup",
      "kafka-cluster:DescribeGroup"
    ]
    resources = [
      "arn:aws:kafka:${var.aws_region}:${data.aws_caller_identity.current.account_id}:group/${module.msk.cluster_name}/*"
    ]
  }
}

resource "aws_iam_policy" "msk_policy" {
  name   = "${var.project_name}-msk-access"
  policy = data.aws_iam_policy_document.msk_policy.json
}

# Required by EKS control plane
# resource "aws_iam_service_linked_role" "eks" {
#   aws_service_name = "eks.amazonaws.com"
# }
