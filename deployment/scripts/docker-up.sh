#!/bin/bash

# OntoMesh Docker Compose Fresh Start Script
# This script cleans up old containers/images and starts fresh

set -e  # Exit on any error

echo "🐳 Starting OntoMesh Docker Compose Fresh Start..."

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

# Stop and remove existing containers
print_status "Stopping existing containers..."
docker compose down --remove-orphans

# Start Kafka cluster separately
print_status "Starting Kafka multi-node cluster..."
cd ../kafka
docker compose down --remove-orphans
docker compose up -d
print_success "Kafka cluster started"

# Wait for Kafka cluster to be ready
print_status "Waiting for Kafka cluster to be ready..."
sleep 30

# Verify Kafka cluster is running
print_status "Verifying Kafka cluster status..."
docker compose ps

cd ../application

# Remove old OntoMesh images
print_status "Removing old OntoMesh images..."
docker images | grep -E "(application-config-server|application-service-discovery|1r-client|1r-processor|1r-server)" | awk '{print $3}' | xargs -r docker rmi -f

# Build and start Docker containers with fresh builds
print_status "Building and starting Docker containers..."
docker compose up --build --force-recreate -d

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 30

# Check service status
print_status "Checking service status..."
docker compose ps

print_success "OntoMesh Docker deployment completed!"
print_status "Access points:"
echo "  - Config Server: http://localhost:8888 (credentials: configuser/configpass) (credentials: configuser/configpass)"
echo "  - Service Discovery: http://localhost:8761 (credentials: eureka/server)"
echo "  - 1R Client: http://localhost:8081"
echo "  - 1R Processor: http://localhost:8083"
echo "  - 1R Server: http://localhost:8085"
echo "  - Kafka Cluster:"
echo "    - Broker 1: localhost:29092"
echo "    - Broker 2: localhost:39092"
echo "    - Broker 3: localhost:49092"
echo "  - GraphDB: http://localhost:7200"
echo "  - Keycloak: https://localhost:8443 (credentials: admin/admin) (credentials: admin/admin)"

print_status "To view logs: docker compose logs -f [service-name]"
print_status "To stop: docker compose down" 