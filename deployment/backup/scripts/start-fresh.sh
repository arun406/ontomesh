#!/bin/bash

# OntoMesh Fresh Start Script
# This script builds all code, cleans up old images, and starts fresh containers

set -e  # Exit on any error

echo "🚀 Starting OntoMesh Fresh Build and Deploy..."

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

# Function to build a Maven project
build_maven_project() {
    local project_path=$1
    local project_name=$2
    
    print_status "Building $project_name..."
    cd "$project_path"
    
    if [ -f "pom.xml" ]; then
        mvn clean package -DskipTests
        print_success "$project_name built successfully"
    else
        print_warning "No pom.xml found in $project_path, skipping build"
    fi
    
    cd - > /dev/null
}

# Stop and remove existing containers
print_status "Stopping existing containers..."
cd ../application
docker compose down --remove-orphans
cd ../scripts

# Start Kafka cluster using the dedicated script
print_status "Starting Kafka cluster using run-kafka.sh..."
./run-kafka.sh

# Ask user if they want to build the source code
echo -e "${YELLOW}Do you want to build the source code? This will compile all Maven projects and remove old images.${NC}"
read -p "Build source code? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Remove old images only when building source code
    print_status "Removing old OntoMesh images..."
    docker images | grep -E "(application-config-server|application-service-discovery|1r-client|1r-processor|1r-server)" | awk '{print $3}' | xargs -r docker rmi -f
    
    print_status "Building all Maven projects..."
    
    # Build config server
    build_maven_project "../../ontomesh-configserver" "Config Server"
    
    # Build service discovery
    build_maven_project "../../ontomesh-servicediscovery" "Service Discovery"
    
    # Build 1r-client
    build_maven_project "../../1r-client" "1R Client"
    
    # Build 1r-event-processor
    build_maven_project "../../1r-event-processor" "1R Event Processor"
    
    # Build 1r-server
    build_maven_project "../../1r-server" "1R Server"
    
    # Build ontomesh-core (dependency)
    build_maven_project "../../ontomesh-core" "OntoMesh Core"
    
    # Build ontomesh-webclient (dependency)
    build_maven_project "../../ontomesh-webclient" "OntoMesh Web Client"
    
    print_success "All Maven projects built successfully"
else
    print_warning "Skipping source code build and image removal. Using existing compiled artifacts and images."
fi

# Build and start Docker containers
print_status "Building and starting Docker containers..."
cd ../application
docker compose up --build --force-recreate -d
cd ../scripts

# Wait for services to be ready
print_status "Waiting for services to be ready..."
sleep 30

# Check service status
print_status "Checking service status..."
cd ../application
docker compose ps

# Show logs for any unhealthy services
print_status "Checking for unhealthy services..."
if docker compose ps | grep -q "unhealthy"; then
    print_warning "Some services are unhealthy. Checking logs..."
    docker compose ps | grep "unhealthy" | while read line; do
        service_name=$(echo $line | awk '{print $1}')
        print_warning "Service $service_name is unhealthy. Recent logs:"
        docker compose logs --tail=10 $service_name
        echo ""
    done
else
    print_success "All services are healthy!"
fi
cd ../scripts

print_success "OntoMesh deployment completed!"
print_status "Access points:"
echo "  - Config Server: http://localhost:8888 (credentials: configuser/configpass)"
echo "  - Service Discovery: http://localhost:8761 (credentials: eureka/server)"
echo "  - 1R Client: http://localhost:8081"
echo "  - 1R Processor: http://localhost:8083"
echo "  - 1R Server: http://localhost:8085"
echo "  - Kafka: localhost:9092"
echo "  - GraphDB: http://localhost:7200"
echo "  - Keycloak: https://localhost:8443 (credentials: admin/admin)"

print_status "To view logs: docker compose logs -f [service-name]"
print_status "To stop: docker compose down" 