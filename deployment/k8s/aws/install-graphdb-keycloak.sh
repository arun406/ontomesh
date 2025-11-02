#!/bin/bash
set -e

# ===============================
# VARIABLES
# ===============================
CLUSTER_NAME="ontomesh"
NODEGROUP_NAME="ontomesh-ng-public1"
REGION=$(aws configure get region)
EBS_CSI_SA_NAME="ebs-csi-controller-sa"
GRAPHDB_NAMESPACE="graphdb"
GRAPHDB_RELEASE="graphdb"
STORAGE_CLASS="gp2"
GRAPHDB_PVC_SIZE="2Gi"
AWS_ACCOUNT_ID=077405089348
KAFKA_SA_NAME=ontonesh-kafka-client-sa

# ===============================
# Step 1: Create EKS cluster (without nodegroup)
# ===============================
echo "Creating EKS cluster $CLUSTER_NAME (without nodegroup)..."
eksctl create cluster \
  --name $CLUSTER_NAME \
  --region $REGION \
  --without-nodegroup \
  --version 1.34 \
  --managed

VPC_ID=$(aws eks describe-cluster --name ontomesh --region $REGION --query "cluster.resourcesVpcConfig.vpcId"  --output text)

# ===============================
# Step 1.5: Associate OIDC provider for IRSA
# ===============================
echo "Associating OIDC provider for cluster $CLUSTER_NAME..."
eksctl utils associate-iam-oidc-provider \
  --cluster $CLUSTER_NAME \
  --region $REGION \
  --approve

# ===============================
# Step 2: Create managed nodegroup
# ===============================
echo "Creating managed nodegroup $NODEGROUP_NAME..."
eksctl create nodegroup \
  --cluster $CLUSTER_NAME \
  --region $REGION \
  --name $NODEGROUP_NAME \
  --node-type t3.large \
  --nodes 1 \
  --nodes-min 1 \
  --nodes-max 2 \
  --node-volume-size 20 \
  --ssh-access \
  --ssh-public-key OneRecord-Key-Pair \
  --managed \
  --asg-access \
  --external-dns-access \
  --full-ecr-access \
  --appmesh-access \
  --alb-ingress-access 
#  --disable-eviction

# ===============================
# Step 3: Create IAM service account for EBS CSI driver
# ===============================
echo "Creating IAM service account for EBS CSI driver..."
eksctl create iamserviceaccount \
  --cluster $CLUSTER_NAME \
  --namespace kube-system \
  --name $EBS_CSI_SA_NAME \
  --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
  --approve \
  --override-existing-serviceaccounts

# ===============================
# Step 4: Install EBS CSI driver addon (let eksctl detect IRSA)
# ===============================
echo "Installing EBS CSI driver addon..."
eksctl create addon \
  --name aws-ebs-csi-driver \
  --cluster $CLUSTER_NAME \
  --region $REGION \
  --force

kubectl get pods -n kube-system | grep ebs

# Optional: migrate to pod identity
# eksctl utils migrate-to-pod-identity --cluster $CLUSTER_NAME

# ===============================
# Step 5: Set default StorageClass to gp2
# ===============================
echo "Setting default StorageClass to $STORAGE_CLASS..."
kubectl patch storageclass $STORAGE_CLASS -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

# ===============================
# Step 6: Install GraphDB using Helm with 2GB storage
# ===============================
echo "Installing GraphDB Helm release with 2GB PVCs..."
kubectl create namespace $GRAPHDB_NAMESPACE || true

helm repo add ontotext https://helm.ontotext.com
helm repo update

# Pod resource requests/limits
GRAPHDB_CPU_REQUEST="1"
GRAPHDB_CPU_LIMIT="2"
GRAPHDB_MEMORY_REQUEST="2Gi"
GRAPHDB_MEMORY_LIMIT="4Gi"

helm install $GRAPHDB_RELEASE ontotext/graphdb \
  --namespace $GRAPHDB_NAMESPACE \
  --set serviceAccount.create=true \
  --set service.type=LoadBalancer \
  --set storageClass=$STORAGE_CLASS \
  --set persistence.size=$GRAPHDB_PVC_SIZE \
  --set resources.requests.memory=$GRAPHDB_MEMORY_REQUEST \
  --set resources.limits.memory=$GRAPHDB_MEMORY_LIMIT
 # --set resources.requests.cpu=$GRAPHDB_CPU_REQUEST \
   # --set resources.limits.cpu=$GRAPHDB_CPU_LIMIT \
echo "Deployment complete. GraphDB should be accessible via LoadBalancer service."


#helm uninstall $GRAPHDB_RELEASE -n $GRAPHDB_NAMESPACE
#kubectl get ingress -A
#kubectl delete -f graphdb-ingress.yaml
#kubectl delete -f keycloak-ingress.yaml


#curl -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json
#aws iam create-policy \
    # --policy-name AWSLoadBalancerControllerIAMPolicy \
    # --policy-document file://iam-policy.json

# eksctl create iamserviceaccount \
#     --cluster $CLUSTER_NAME \
#     --namespace kube-system \
#     --name aws-load-balancer-controller \
#     --attach-policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
#     --approve

# helm repo add eks https://aws.github.io/eks-charts
# helm repo update

# helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
#     -n kube-system \
#     --set clusterName=$CLUSTER_NAME \
#     --set serviceAccount.create=false \
#     --set serviceAccount.name=aws-load-balancer-controller \
#     --set region=$REGION

#kubectl get pods -n kube-system | grep aws-load-balancer-controller



# eksctl delete nodegroup   \
# --cluster $CLUSTER_NAME   \
# --name ontomesh-ng-public0   \
# --region $REGION  \
# --disable-eviction || true

# eksctl delete cluster --name $CLUSTER_NAME  --region $REGION 

# helm uninstall onerecord -n onerecord
# helm uninstall graphdb --namespace graphdb
# helm uninstall keycloak -n keycloak

# helm repo add bitnami https://charts.bitnami.com/bitnami
# helm repo update

# kubectl create configmap keycloak-realm-config \
#   --from-file=ontomesh-realm.json=ontomesh-realm.json \
#   -n keycloak \
#   -o yaml --dry-run=client > keycloak-realm-config.yaml

# kubectl apply -f keycloak-realm-config.yaml


helm install keycloak bitnami/keycloak \
  --version 24.4.9 -n keycloak \
  --create-namespace -f keycloak-values.yaml \
  --set image.repository=bitnamilegacy/keycloak \
  --set postgresql.image.repository=bitnamilegacy/postgresql \
  --set global.security.allowInsecureImages=true

#helm uninstall keycloak -n keycloak
aws eks describe-cluster --name ontomesh --region $REGION --output yaml

#Get the subnets from the above cluster describe command. 
aws ec2 describe-subnets \
  --subnet-ids subnet-034ff4991c7481a51 subnet-07d5a41bd2c34161f subnet-094cc144c87cb1985 subnet-09d80dab6f85e0707 subnet-0e85765572440bfbe subnet-03fee55e9b5b19500 \
  --region ap-south-2 \
  --query 'Subnets[*].{ID:SubnetId,Public:MapPublicIpOnLaunch,AZ:AvailabilityZone}' \
  --output table

aws eks describe-nodegroup --cluster-name ontomesh --nodegroup-name ontomesh-ng-public1 --output yaml


VPC_ID=$(aws eks describe-cluster --name ontomesh --region $REGION --query "cluster.resourcesVpcConfig.vpcId"  --output text)

aws ec2 describe-security-groups \
--filters Name=vpc-id,Values=$VPC_ID \
--region $REGION \
--query "SecurityGroups[*].[GroupId,GroupName]" \
--output text
# sg-0ed71544a55756b58    k8s-elb-ace11eb9dcb4343259ecd32238cc517e
# sg-06bcb26ffb47d1329    default
# sg-03681f815a62da441    eksctl-ontomesh-cluster-ClusterSharedNodeSecurityGroup-sK8YDqfBwwtn -- take this.
# sg-008d57dd43e0378e0    eks-cluster-sg-ontomesh-1848551033 
# sg-08d744db222e94fb6    eksctl-ontomesh-nodegroup-ontomesh-ng-public1-remoteAccess
# sg-0d0c146f51bcee67a    eksctl-ontomesh-cluster-ControlPlaneSecurityGroup-6cWGwRvBWCbX
# sg-0e70c4e4ed197904e    k8s-elb-a62a0efdf6b3e4ecc9ce4e46d87bd109


aws ec2 describe-subnets \
  --subnet-ids $(aws eks describe-cluster \
    --name ontomesh \
    --region $REGION \
    --query "cluster.resourcesVpcConfig.subnetIds[]" \
    --output text) \
  --region $REGION \
  --query 'Subnets[?MapPublicIpOnLaunch==`false`].{ID:SubnetId,AZ:AvailabilityZone}' \
  --output table
#   ---------------------------------------------
# |              DescribeSubnets              |
# +--------------+----------------------------+
# |      AZ      |            ID              |
# +--------------+----------------------------+
# |  ap-south-2c |  subnet-0e85765572440bfbe  |
# |  ap-south-2a |  subnet-03fee55e9b5b19500  |
# |  ap-south-2b |  subnet-09d80dab6f85e0707  |
# +--------------+----------------------------+

MSK_NAME=ontomesh

#prepare below with the security group and subnet ids
aws kafka create-cluster-v2 \
--region $REGION \
--cluster-name $MSK_NAME \
--provisioned '{
  "BrokerNodeGroupInfo": {
    "BrokerAZDistribution": "DEFAULT",
    "ClientSubnets": [
      "subnet-0e85765572440bfbe",
      "subnet-03fee55e9b5b19500",
      "subnet-09d80dab6f85e0707"
    ],
    "InstanceType": "kafka.t3.small",
    "SecurityGroups": ["sg-03681f815a62da441"],
    "StorageInfo": {
      "EbsStorageInfo": {
        "ProvisionedThroughput": {
          "Enabled": false
        },
        "VolumeSize": 5
      }
    }
  },
  "ClientAuthentication": {
    "Sasl": {
      "Iam": {
        "Enabled": true
      }
    }
  },
  "EnhancedMonitoring": "DEFAULT",
  "KafkaVersion": "3.8.x",
  "NumberOfBrokerNodes": 3
}
'
# {
#     "ClusterArn": "arn:aws:kafka:ap-south-2:077405089348:cluster/ontomesh/e4cd8c63-fce3-4ae3-8708-96278ab2887d-2",
#     "ClusterName": "ontomesh",
#     "State": "CREATING",
#     "ClusterType": "PROVISIONED"
# }

aws iam create-policy \
  --policy-name MSKClientPolicy \
  --policy-document file://msk-access-policy.json

aws iam list-policies \
  --query 'Policies[?PolicyName==`MSKClientPolicy`]' \
  --output table

# ---------------------------------------------------------------------------------------
# |                                    ListPolicies                                     |
# +--------------------------------+----------------------------------------------------+
# |  Arn                           |  arn:aws:iam::077405089348:policy/MSKClientPolicy  |
# |  AttachmentCount               |  0                                                 |
# |  CreateDate                    |  2025-11-01T11:06:40+00:00                         |
# |  DefaultVersionId              |  v2                                                |
# |  IsAttachable                  |  True                                              |
# |  Path                          |  /                                                 |
# |  PermissionsBoundaryUsageCount |  0                                                 |
# |  PolicyId                      |  ANPAREBNT5ZCIZQTJRZGO                             |
# |  PolicyName                    |  MSKClientPolicy                                   |
# |  UpdateDate                    |  2025-11-01T11:34:24+00:00                         |
# +--------------------------------+----------------------------------------------------+

# aws iam get-policy --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/MSKClientPolicy
eksctl create iamserviceaccount \
  --name $KAFKA_SA_NAME \
  --namespace default \
  --cluster $CLUSTER_NAME \
  --attach-policy-arn arn:aws:iam::077405089348:policy/MSKClientPolicy \
  --approve \
  --region $REGION


  docker buildx build \
  --platform linux/amd64 \
  -t arun406/1r-server:latest \
  .

  docker push arun406/1r-server:latest


# /opt/bitnami/kafka/bin/kafka-topics.sh \
#   --bootstrap-server b-1.ontomesh.3xr24v.c2.kafka.ap-south-2.amazonaws.com:9098,b-2.ontomesh.3xr24v.c2.kafka.ap-south-2.amazonaws.com:9098,b-3.ontomesh.3xr24v.c2.kafka.ap-south-2.amazonaws.com:9098 \
#   --command-config /opt/kafka/config/client.properties