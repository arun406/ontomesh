#!/bin/bash

# Port availability and service connectivity checker
# This script checks if all required ports are available and services are accessible

set -e

echo "Checking OntoMesh port configuration and service connectivity..."
echo ""

# Define ports to check using simple arrays
services=("Config_Server" "Service_Discovery" "OneR_Client" "OneR_Processor" "OneR_Server" "Kafka" "Keycloak" "GraphDB_REST" "GraphDB_Workbench")
ports=("8888" "8761" "8081" "8083" "8085" "9092" "8443" "7200" "7300")

# Check port availability
echo "=== Port Availability Check ==="
all_ports_available=true

for i in "${!services[@]}"; do
    service="${services[$i]}"
    port="${ports[$i]}"
    
    # Convert underscore back to space for display
    display_name=$(echo "$service" | sed 's/_/ /g')
    
    # Check if port is in use
    if lsof -i :"$port" >/dev/null 2>&1; then
        echo "⚠  Port $port ($display_name) is in use"
        all_ports_available=false
    else
        echo "✓  Port $port ($display_name) is available"
    fi
done

echo ""

# Check service connectivity (if services are running)
echo "=== Service Connectivity Check ==="

# Check if Docker Compose is running
if ! docker compose ps | grep -q "Up"; then
    echo "ℹ  Docker Compose services are not running"
    echo "   Start services with: docker compose up -d"
    echo ""
    echo "Port availability check completed."
    if [ "$all_ports_available" = true ]; then
        echo "✓ All ports are available for use."
        exit 0
    else
        echo "⚠ Some ports are already in use. Please free them before starting services."
        exit 1
    fi
fi

# Check service health
echo "Checking service health..."

# Application services
for service in "config-server" "service-discovery" "1r-client" "1r-processor" "1r-server"; do
    # Map service names to display names
    case $service in
        "config-server")
            display_name="Config Server"
            port="8888"
            ;;
        "service-discovery")
            display_name="Service Discovery"
            port="8761"
            ;;
        "1r-client")
            display_name="1r-client"
            port="8081"
            ;;
        "1r-processor")
            display_name="1r-processor"
            port="8083"
            ;;
        "1r-server")
            display_name="1r-server"
            port="8085"
            ;;
    esac
    
    if curl -s -f "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
        echo "✓ $display_name (port $port) is healthy"
    else
        echo "⚠ $display_name (port $port) is not responding"
    fi
done

# Infrastructure services
if curl -s -f "http://localhost:7200/rest/repositories" >/dev/null 2>&1; then
    echo "✓ GraphDB REST (port 7200) is healthy"
else
    echo "⚠ GraphDB REST (port 7200) is not responding"
fi

if curl -s -f "http://localhost:7300" >/dev/null 2>&1; then
    echo "✓ GraphDB Workbench (port 7300) is accessible"
else
    echo "⚠ GraphDB Workbench (port 7300) is not accessible"
fi

if curl -s -f "https://localhost:8443/health/ready" -k >/dev/null 2>&1; then
    echo "✓ Keycloak (port 8443) is healthy"
else
    echo "⚠ Keycloak (port 8443) is not responding"
fi

# Kafka check (requires docker exec)
if docker compose exec -T kafka kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
    echo "✓ Kafka (port 9092) is healthy"
else
    echo "⚠ Kafka (port 9092) is not responding"
fi

echo ""
echo "=== Summary ==="
if [ "$all_ports_available" = true ]; then
    echo "✓ All ports are available"
else
    echo "⚠ Some ports are in use"
fi

echo ""
echo "For detailed port configuration, see ports.md"
echo "For troubleshooting, see README.md" 