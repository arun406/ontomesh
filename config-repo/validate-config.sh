#!/bin/bash

# Configuration validation script for OntoMesh config-repo
# This script validates the configuration files and environment variables

set -e

echo "Validating OntoMesh Configuration Repository..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1 (missing)"
        return 1
    fi
}

# Function to check for environment variable patterns
check_env_vars() {
    local file=$1
    local pattern=$2
    local description=$3
    
    if grep -q "$pattern" "$file"; then
        echo -e "${GREEN}✓${NC} $description found in $file"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} $description not found in $file"
        return 1
    fi
}

# Function to check for old configuration patterns
check_old_config() {
    local file=$1
    local pattern=$2
    local description=$3
    
    if grep -q "$pattern" "$file"; then
        echo -e "${RED}✗${NC} $description found in $file (needs update)"
        return 1
    else
        echo -e "${GREEN}✓${NC} $description not found in $file (already updated)"
        return 0
    fi
}

echo "=== File Structure Validation ==="

# Check main configuration files
check_file "application.yml"
check_file "client/application.yml"
check_file "client/application-kafka.yml"
check_file "processor/application.yml"
check_file "processor/application-kafka.yml"
check_file "processor/application-rdf4j.yml"
check_file "server/application.yml"
check_file "server/application-kafka.yml"
check_file "server/application-rdf4j.yml"
check_file "servicediscovery/application.yml"

echo ""

echo "=== Environment Variable Validation ==="

# Check for proper environment variable usage
check_env_vars "application.yml" "SERVICE_DISCOVERY_URL" "Service discovery URL"
check_env_vars "client/application.yml" "ONERECORD_CLIENT_PORT" "Client port configuration"
check_env_vars "client/application-kafka.yml" "KAFKA_BOOTSTRAP_SERVERS" "Kafka bootstrap servers"
check_env_vars "processor/application.yml" "ONERECORD_PROCESSOR_PORT" "Processor port configuration"
check_env_vars "processor/application-kafka.yml" "KAFKA_BOOTSTRAP_SERVERS" "Kafka bootstrap servers"
check_env_vars "processor/application-rdf4j.yml" "REPOSITORY_URL" "GraphDB repository URL"
check_env_vars "server/application.yml" "ONERECORD_SERVER_PORT" "Server port configuration"
check_env_vars "server/application.yml" "ISSUER_URI" "OAuth2 issuer URI"
check_env_vars "server/application-kafka.yml" "KAFKA_BOOTSTRAP_SERVERS" "Kafka bootstrap servers"
check_env_vars "server/application-rdf4j.yml" "REPOSITORY_URL" "GraphDB repository URL"
check_env_vars "servicediscovery/application.yml" "SERVICE_DISCOVERY_SERVER_PORT" "Service discovery port"

echo ""
echo "=== Config Server Configuration Validation ==="

# Check Config Server configuration
echo "Checking Config Server searchLocations configuration..."
if grep -q "file:/app/config-repo" ../ontomesh-configserver/src/main/resources/application.yml; then
    echo -e "${GREEN}✓${NC} Config Server searchLocations uses correct container path"
else
    echo -e "${RED}✗${NC} Config Server searchLocations doesn't use correct container path"
fi

# Check if SEARCH_LOCATIONS is properly configured
if grep -q "SEARCH_LOCATIONS" ../deployment/application/docker-compose.yml; then
    echo -e "${GREEN}✓${NC} SEARCH_LOCATIONS environment variable is configured in docker-compose.yml"
else
    echo -e "${RED}✗${NC} SEARCH_LOCATIONS environment variable is not configured in docker-compose.yml"
fi

if grep -q "SEARCH_LOCATIONS" ../deployment/application/env.example; then
    echo -e "${GREEN}✓${NC} SEARCH_LOCATIONS environment variable is documented in env.example"
else
    echo -e "${RED}✗${NC} SEARCH_LOCATIONS environment variable is not documented in env.example"
fi

echo ""

echo "=== Configuration Update Validation ==="

# Check for old configuration patterns that should be updated
check_old_config "client/application-kafka.yml" "localhost:29092" "Old multi-broker Kafka config"
check_old_config "processor/application-kafka.yml" "localhost:29092" "Old multi-broker Kafka config"
check_old_config "server/application-kafka.yml" "localhost:29092" "Old multi-broker Kafka config"
check_old_config "processor/application-rdf4j.yml" "GGKMAC23066" "Old GraphDB localhost reference"
check_old_config "server/application-rdf4j.yml" "GGKMAC23066" "Old GraphDB localhost reference"

echo ""

echo "=== Configuration Content Validation ==="

# Validate specific configuration values
echo "Checking Kafka configuration..."

# Check if Kafka uses single-node setup
if grep -q "kafka:9092" client/application-kafka.yml && \
   grep -q "kafka:9092" processor/application-kafka.yml && \
   grep -q "kafka:9092" server/application-kafka.yml; then
    echo -e "${GREEN}✓${NC} All Kafka configurations use single-node setup (kafka:9092)"
else
    echo -e "${RED}✗${NC} Some Kafka configurations don't use single-node setup"
fi

# Check if GraphDB uses correct URL
if grep -q "graphdb:7200" processor/application-rdf4j.yml && \
   grep -q "graphdb:7200" server/application-rdf4j.yml; then
    echo -e "${GREEN}✓${NC} All GraphDB configurations use correct URL (graphdb:7200)"
else
    echo -e "${RED}✗${NC} Some GraphDB configurations don't use correct URL"
fi

# Check service discovery configuration
if grep -q "service-discovery:8080" application.yml; then
    echo -e "${GREEN}✓${NC} Service discovery uses correct container name"
else
    echo -e "${RED}✗${NC} Service discovery doesn't use correct container name"
fi

echo ""

echo "=== Configuration Profiles Validation ==="

# Check for profile-specific configurations
profiles=("dev" "kafka" "rdf4j" "https" "onerecord" "airline-clients" "forwarder-clients" "airline-onerecord" "forwarder-onerecord")

for profile in "${profiles[@]}"; do
    if find . -name "*$profile*.yml" | grep -q .; then
        echo -e "${GREEN}✓${NC} Profile '$profile' configuration found"
    else
        echo -e "${YELLOW}⚠${NC} Profile '$profile' configuration not found"
    fi
done

echo ""

echo "=== Environment Variable Summary ==="

# Extract and display all environment variables used
echo "Environment variables used in configuration:"
grep -r "\${[A-Z_]*}" . --include="*.yml" | sed 's/.*\${\([^}]*\)}.*/\1/' | sort | uniq | while read var; do
    echo "  - $var"
done

echo ""

echo "=== Validation Complete ==="
echo ""
echo "Configuration validation completed. Check the output above for any issues."
echo ""
echo "To test the configuration with Config Server:"
echo "1. Start the Config Server: docker-compose up config-server"
echo "2. Test configuration endpoints:"
echo "   curl http://localhost:8888/client/dev"
echo "   curl http://localhost:8888/server/kafka"
echo "   curl http://localhost:8888/processor/rdf4j" 