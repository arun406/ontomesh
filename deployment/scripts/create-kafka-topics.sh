#!/bin/bash

# Kafka topic creation script for OntoMesh local development
# Run this after Kafka is up, before starting services

set -e

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

BROKER=localhost:9092
KAFKA_TOPICS=(
  "logistics-events"
  "logistics-events-notification"
)

print_status "Creating Kafka topics..."

for TOPIC in "${KAFKA_TOPICS[@]}"; do
  print_status "Creating topic: $TOPIC"
  docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BROKER --create --if-not-exists --topic "$TOPIC" --partitions 1 --replication-factor 1 || true
done

print_success "All required topics created (or already exist)."

# List all topics to verify creation
print_status "Listing all Kafka topics:"
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server $BROKER --list

print_success "Kafka topic creation completed!" 