#!/bin/bash

# Kafka Cluster Startup Script
# This script starts the multi-node Kafka cluster

set -e  # Exit on any error

echo "🚀 Starting Kafka Multi-Node Cluster..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Stop existing Kafka cluster
print_status "Stopping existing Kafka cluster..."
cd ../kafka
docker compose down --remove-orphans

# Start Kafka cluster
print_status "Starting Kafka multi-node cluster..."
docker compose up -d

# Wait for Kafka cluster to be ready
print_status "Waiting for Kafka cluster to be ready..."
sleep 30

# Verify Kafka cluster is running
print_status "Verifying Kafka cluster status..."
docker compose ps

# Check if all containers are healthy
if docker compose ps | grep -q "unhealthy"; then
    print_warning "Some Kafka containers are unhealthy. Checking logs..."
    docker compose ps | grep "unhealthy" | while read line; do
        service_name=$(echo $line | awk '{print $1}')
        print_warning "Service $service_name is unhealthy. Recent logs:"
        docker compose logs --tail=10 $service_name
        echo ""
    done
else
    print_success "All Kafka containers are healthy!"
fi

cd ../application

print_success "Kafka cluster started successfully!"
print_status "Kafka Cluster Access Points:"
echo "  - Broker 1: localhost:29092"
echo "  - Broker 2: localhost:39092"
echo "  - Broker 3: localhost:49092"
echo ""
print_status "To view Kafka logs: cd ../kafka && docker compose logs -f [service-name]"
print_status "To stop Kafka: cd ../kafka && docker compose down" 