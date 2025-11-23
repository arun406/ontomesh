admin_cidr               = "0.0.0.0/0" # "106.222.233.80/32" #curl -4 ifconfig.me
eks_version              = "1.34"
eks_node_instance_type   = "t3.large"
ssh_key_name             = "OneRecord-Key-Pair"
msk_broker_instance_type = "kafka.t3.small"
msk_num_brokers          = 2
ebs_volume_size          = 5
