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

# Function to stop all running containers
stop_all_containers() {
    print_status "Stopping all running containers..."
    
    # Get list of running containers
    local running_containers=$(docker ps -q)
    
    if [[ -n "$running_containers" ]]; then
        print_status "Found running containers, stopping them..."
        docker stop $running_containers
        print_success "All containers stopped"
    else
        print_status "No running containers found"
    fi
}

# Function to remove all containers
remove_all_containers() {
    print_status "Removing all containers..."
    
    # Get list of all containers (including stopped ones)
    local all_containers=$(docker ps -aq)
    
    if [[ -n "$all_containers" ]]; then
        print_status "Found containers, removing them..."
        docker rm $all_containers
        print_success "All containers removed"
    else
        print_status "No containers found"
    fi
}

# Stop and remove application containers
print_status "Stopping application containers..."
cd ../application
docker compose down --remove-orphans --volumes

# Stop and remove Kafka cluster
print_status "Stopping Kafka cluster..."
cd ../kafka
docker compose down --remove-orphans --volumes
cd ../application

# Ensure all containers are stopped and removed
stop_all_containers
remove_all_containers

# Wait a moment to ensure all containers are fully stopped
print_status "Waiting for containers to fully stop..."
sleep 3

# Verify no containers are running
running_count=$(docker ps -q | wc -l)
if [[ $running_count -gt 0 ]]; then
    print_warning "Some containers are still running. Attempting to force stop..."
    docker ps -q | xargs -r docker stop --time=10
    sleep 2
fi

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