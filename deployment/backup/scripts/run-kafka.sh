#!/bin/bash

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

# Function to check if Kafka container is running
check_kafka_status() {
    if docker ps --format "table {{.Names}}" | grep -q "^kafka$"; then
        return 0  # Kafka is running
    else
        return 1  # Kafka is not running
    fi
}

# Function to check if Kafka is healthy (responding to connections)
check_kafka_health() {
    if check_kafka_status; then
        # Try to connect to Kafka and list topics
        if docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
            return 0  # Kafka is healthy
        else
            return 1  # Kafka is running but not healthy
        fi
    else
        return 1  # Kafka is not running
    fi
}

# Function to get user confirmation
get_user_confirmation() {
    local message="$1"
    local default_action="$2"
    
    if [ "$default_action" = "restart" ]; then
        echo -e "${YELLOW}$message${NC}"
        read -p "Do you want to restart Kafka? (y/N): " -n 1 -r
        echo
    else
        echo -e "${YELLOW}$message${NC}"
        read -p "Do you want to start Kafka? (y/N): " -n 1 -r
        echo
    fi
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        return 0
    else
        return 1
    fi
}

# Function to ensure ontomesh-network exists
ensure_network() {
    print_status "Ensuring ontomesh-network exists..."
    docker network create ontomesh-network 2>/dev/null || true
}

# Function to create Kafka topics
create_kafka_topics() {
    print_status "Creating Kafka topics..."
    cd ../kafka
    if [ -f "../scripts/create-kafka-topics.sh" ]; then
        ../scripts/create-kafka-topics.sh
        print_success "Kafka topics created successfully"
    else
        print_warning "create-kafka-topics.sh not found, skipping topic creation"
    fi
    cd ../scripts
}

# Function to start Kafka cluster
start_kafka_cluster() {
    print_status "Starting Kafka single-node cluster..."
    cd ../kafka
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
    
    # Wait a bit more for Kafka to be fully ready
    print_status "Waiting for Kafka to be fully ready before creating topics..."
    sleep 10
    
    cd ../scripts
}

# Function to stop Kafka cluster
stop_kafka_cluster() {
    print_status "Stopping existing Kafka cluster..."
    cd ../kafka
    docker compose down --remove-orphans
    cd ../scripts
}

# Main script logic
main() {
    print_status "Checking Kafka cluster status..."
    
    if check_kafka_health; then
        print_success "Kafka cluster is running and healthy"
        
        # Show Kafka container details
        echo
        print_status "Kafka container details:"
        docker ps --filter "name=kafka" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        
        # Show Kafka topics
        echo
        print_status "Available Kafka topics:"
        docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null || print_warning "Could not list topics"
        
        echo
        if get_user_confirmation "Kafka is already running. Do you want to restart it?" "restart"; then
            print_status "Restarting Kafka cluster..."
            stop_kafka_cluster
            ensure_network
            start_kafka_cluster
            create_kafka_topics
            print_success "Kafka cluster restarted successfully"
        else
            print_status "Kafka cluster left running"
        fi
        
    elif check_kafka_status; then
        print_warning "Kafka container is running but not responding to connections"
        
        if get_user_confirmation "Kafka appears to be in an unhealthy state. Do you want to restart it?" "restart"; then
            print_status "Restarting Kafka cluster..."
            stop_kafka_cluster
            ensure_network
            start_kafka_cluster
            create_kafka_topics
            print_success "Kafka cluster restarted successfully"
        else
            print_status "Kafka cluster left in current state"
        fi
        
    else
        print_warning "Kafka cluster is not running"
        
        if get_user_confirmation "Do you want to start the Kafka cluster?" "start"; then
            print_status "Starting Kafka cluster..."
            ensure_network
            start_kafka_cluster
            create_kafka_topics
            
            if check_kafka_health; then
                print_success "Kafka cluster started successfully"
                
                # Show available topics
                echo
                print_status "Available Kafka topics:"
                docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null || print_warning "Could not list topics"
                
                print_status "Kafka Cluster Access Points:"
                echo "  - Broker: localhost:9092"
                echo ""
                print_status "To view Kafka logs: cd ../kafka && docker compose logs -f kafka"
                print_status "To stop Kafka: cd ../kafka && docker compose down"
            else
                print_error "Kafka cluster failed to start properly"
                print_status "Check logs with: docker logs kafka"
            fi
        else
            print_status "Kafka cluster not started"
        fi
    fi
}

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed or not in PATH"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    print_error "Docker is not running or not accessible"
    exit 1
fi

# Run main function
main "$@" 