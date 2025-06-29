#!/bin/bash

# OntoMesh Cleanup Script
# This script stops all containers and removes volumes

set -e  # Exit on any error

echo "🧹 Starting OntoMesh Cleanup..."

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

# Function to confirm action
confirm_action() {
    local message=$1
    echo -e "${YELLOW}$message${NC}"
    read -p "Do you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Cleanup cancelled."
        exit 0
    fi
}

# Stop and remove application containers
print_status "Stopping application containers..."
docker compose down --remove-orphans --volumes

# Stop and remove Kafka cluster
print_status "Stopping Kafka cluster..."
cd ../kafka
docker compose down --remove-orphans --volumes
cd ../application

# Remove all OntoMesh images
print_status "Removing OntoMesh images..."
docker images | grep -E "(application-config-server|application-service-discovery|1r-client|1r-processor|1r-server)" | awk '{print $3}' | xargs -r docker rmi -f

# Remove Kafka images
print_status "Removing Kafka images..."
docker images | grep "apache/kafka" | awk '{print $3}' | xargs -r docker rmi -f

# Remove unused volumes
print_status "Removing unused volumes..."
docker volume prune -f

# Remove unused networks
print_status "Removing unused networks..."
docker network prune -f

# Show remaining images
print_status "Remaining Docker images:"
docker images

print_success "OntoMesh cleanup completed!"
print_status "To start fresh: ./docker-up.sh or ./start-fresh.sh" 