# OntoMesh ONE Record Platform Documentation

Welcome to the OntoMesh ONE Record platform! This documentation provides everything you need to deploy, configure, and operate the OntoMesh microservices ecosystem for IATA ONE Record digital cargo data exchange.

---

## Table of Contents
- [What is OntoMesh?](#what-is-ontomesh)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [System Architecture](#system-architecture)
- [Deployment Options](#deployment-options)
- [Configuration](#configuration)
- [Verification & Testing](#verification--testing)
- [Troubleshooting](#troubleshooting)
- [Maintenance & Operations](#maintenance--operations)
- [Port Configuration](#port-configuration)
- [Quick Reference](#quick-reference)

---

## What is OntoMesh?

OntoMesh is a complete implementation of the **IATA ONE Record standard** that enables digital data exchange in the air cargo industry. It consists of:

### Core Components
- **ONE Record Server** (Port 8085): Main API server implementing IATA ONE Record standard
- **ONE Record Client** (Port 8081): Client service for external integrations
- **Event Processor** (Port 8083): Processes logistics events and notifications
- **Config Server** (Port 8888): Centralized configuration management
- **Service Discovery** (Port 8761): Eureka-based service registration

### Infrastructure Services
- **GraphDB** (Port 7200): Semantic data storage using RDF/SPARQL
- **Apache Kafka** (Port 9092): Event streaming platform
- **Keycloak** (Port 8443): Identity and access management
- **PostgreSQL**: Database for Keycloak

---

## Prerequisites

### System Requirements
- **Operating System**: macOS, Linux, or Windows with WSL2
- **Docker**: Version 20.10+ with Docker Compose
- **RAM**: Minimum 6GB available for Docker containers
- **Disk Space**: At least 10GB free space
- **Network**: Ports 8081, 8083, 8085, 8761, 8888, 7200, 8443, 9092 must be available

### Required Software

#### 1. Docker and Docker Compose
```bash
# Check Docker version
docker --version
docker compose version

# If not installed, follow official Docker installation guide
# https://docs.docker.com/get-docker/
```

#### 2. Java 21+ (Optional - only if using Maven builds)
```bash
# Check Java version
java --version

# If not installed, download from:
# https://adoptium.net/temurin/releases/
```

#### 3. Maven (Optional - only if using Maven builds)
```bash
# Check Maven version
mvn --version

# If not installed, download from:
# https://maven.apache.org/download.cgi
```

### Port Availability Check
Before starting, ensure these ports are not in use:

```bash
# Check if ports are available
netstat -an | grep -E "(8081|8083|8085|8761|8888|7200|8443|9092)"

# On macOS/Linux, you can also use:
lsof -i :8081
lsof -i :8083
lsof -i :8085
lsof -i :8761
lsof -i :8888
lsof -i :7200
lsof -i :8443
lsof -i :9092
```

---

## Quick Start

### First Time Setup
```bash
# Navigate to deployment directory
cd /Users/arun/source/ontomesh/deployment

# Generate SSL certificates
cd scripts
./generate-certs.sh

# Check port availability
./check-ports.sh

# Setup environment (interactive)
./setup-env.sh

# Start complete stack
./start-fresh.sh
```

### Subsequent Starts
```bash
# Quick start (Docker only)
./docker-up.sh

# Or manual Docker Compose
cd application
docker compose up -d
```

### Monitor Startup
```bash
# Watch the startup process
docker compose logs -f

# You should see services starting in this order:
# 1. Config Server
# 2. Service Discovery
# 3. Infrastructure (PostgreSQL, Kafka, GraphDB, Keycloak)
# 4. Application services (1r-server, 1r-client, 1r-processor)
```

### Verify Deployment
```bash
# Check all services are running
docker compose ps

# Expected output:
# Name                    Command               State           Ports
# -----------------------------------------------------------------------------
# application-config-server   java -jar /app/app.jar        Up      0.0.0.0:8888->8080/tcp
# application-service-discovery java -jar /app/app.jar        Up      0.0.0.0:8761->8080/tcp
# graphdb                   /opt/graphdb/bin/graphdb         Up      0.0.0.0:7200->7200/tcp
# kafka                     /etc/confluent/docker/run        Up      0.0.0.0:9092->9092/tcp
# keycloak                  /opt/keycloak/bin/kc.sh sta ...  Up      0.0.0.0:8443->8443/tcp
# 1r-client                 java -jar /app/app.jar          Up      0.0.0.0:8081->8080/tcp
# 1r-processor              java -jar /app/app.jar          Up      0.0.0.0:8083->8080/tcp
# 1r-server                 java -jar /app/app.jar          Up      0.0.0.0:8085->8080/tcp
```

---

## System Architecture

### Service Dependencies
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Config Server │    │ Service Discovery│    │   PostgreSQL    │
│   (Port 8888)   │    │   (Port 8761)    │    │   (Port 5432)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   GraphDB       │    │      Kafka       │    │    Keycloak     │
│   (Port 7200)   │    │   (Port 9092)    │    │   (Port 8443)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
         ┌─────────────────────────────────────────────────┐
         │              Application Services               │
         │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
         │  │ 1r-server   │ │ 1r-client   │ │1r-processor │ │
         │  │ (Port 8085) │ │ (Port 8081) │ │(Port 8083)  │ │
         │  └─────────────┘ └─────────────┘ └─────────────┘ │
         └─────────────────────────────────────────────────┘
```

### Service URLs and Access Points

#### Application Services
| Service | URL | Purpose | Credentials |
|---------|-----|---------|-------------|
| Config Server | http://localhost:8888 | Configuration management | `configuser`/`configpass` |
| Service Discovery | http://localhost:8761 | Service registration | `eureka`/`server` |
| Service Discovery UI | http://localhost:8761/eurekawebui | Web interface | `eureka`/`server` |
| ONE Record Server | http://localhost:8085 | Main API server | OAuth2 via Keycloak |
| ONE Record Client | http://localhost:8081 | Client service | OAuth2 via Keycloak |
| Event Processor | http://localhost:8083 | Event processing | OAuth2 via Keycloak |

#### Infrastructure Services
| Service | URL | Purpose | Credentials |
|---------|-----|---------|-------------|
| GraphDB REST | http://localhost:7200 | RDF data storage | `admin`/`admin` |
| GraphDB Workbench | http://localhost:7300 | Web interface | `admin`/`admin` |
| Keycloak | https://localhost:8443 | Identity management | `admin`/`admin` |
| Kafka | localhost:9092 | Event streaming | None (internal) |

### Data Flow
1. **Configuration**: All services get config from Config Server
2. **Registration**: Services register with Service Discovery
3. **Authentication**: Keycloak handles OAuth2 authentication
4. **Data Storage**: GraphDB stores semantic data
5. **Event Processing**: Kafka handles event streaming
6. **API Access**: External clients access ONE Record Server

---

## Deployment Options

### Option 1: Complete Fresh Build (Recommended for First Time)
```bash
# This builds everything from scratch
./start-fresh.sh

# What this does:
# 1. Stops existing containers
# 2. Removes old Docker images
# 3. Builds all Maven projects
# 4. Creates fresh Docker images
# 5. Starts all services
# 6. Shows health status
```

### Option 2: Docker-Only Fresh Build (Faster)
```bash
# This assumes Maven projects are already built
./docker-up.sh

# What this does:
# 1. Stops existing containers
# 2. Removes old Docker images
# 3. Builds fresh Docker images
# 4. Starts all services
```

### Option 3: Manual Docker Compose
```bash
# Start all services
docker compose up -d

# Start with rebuild
docker compose up --build -d

# Start with force recreate
docker compose up --force-recreate -d
```

### Option 4: Individual Service Management
```bash
# Build specific service
docker compose build --no-cache 1r-server

# Start specific service
docker compose up -d 1r-server

# Restart specific service
docker compose restart 1r-server

# View logs for specific service
docker compose logs -f 1r-server
```

### Deployment Scripts Comparison

| Script | Use Case | Builds Maven | Builds Docker | Time |
|--------|----------|--------------|---------------|------|
| `start-fresh.sh` | First time, code changes | ✅ | ✅ | ~10-15 min |
| `docker-up.sh` | Docker changes only | ❌ | ✅ | ~5-10 min |
| `docker compose up` | Configuration changes | ❌ | ❌ | ~2-5 min |

---

## Configuration

### Environment Variables (.env file)

The deployment uses a `.env` file in the `application/` directory for configuration.

**Example .env file:**
```bash
# Global Configuration
ONTOMESH_HOME=/Users/arun/source/ontomesh
SERVICE_DISCOVERY_URL=http://eureka:server@service-discovery:8080/eureka

# Config Server
CONFIG_SERVER_PORT=8080
CONFIG_USERNAME=configuser
CONFIG_PASSWORD=configpass

# Service Discovery
SERVICE_DISCOVERY_SERVER_PORT=8080
EUREKA_USERNAME=eureka
EUREKA_PASSWORD=server

# ONE Record Services
ONERECORD_CLIENT_PORT=8080
ONERECORD_PROCESSOR_PORT=8080
ONERECORD_SERVER_PORT=8080

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# GraphDB Configuration
REPOSITORY_URL=http://graphdb:7200
REPOSITORY_NAME=ontomesh
REPOSITORY_USERNAME=admin
REPOSITORY_PASSWORD=admin
```

### Configuration Profiles

Services use Spring profiles for different environments:

- **`dev`**: Development environment
- **`test`**: Testing environment
- **`prod`**: Production environment
- **`kafka`**: Enable Kafka messaging
- **`rdf4j`**: Enable GraphDB/RDF4J
- **`https`**: Enable HTTPS/TLS
- **`onerecord`**: ONE Record standard compliance

### Configuration Files Structure
```
config-repo/
├── application.yml                    # Global settings
├── client/                           # ONE Record Client config
│   ├── application.yml               # Base client config
│   ├── application-kafka.yml         # Kafka producer config
│   ├── application-airline-clients.yml    # Airline-specific config
│   └── application-forwarder-clients.yml  # Forwarder-specific config
├── processor/                        # Event Processor config
│   ├── application.yml               # Base processor config
│   ├── application-kafka.yml         # Kafka consumer config
│   ├── application-airline-onerecord.yml  # Airline processor config
│   └── application-rdf4j.yml         # GraphDB config
├── server/                           # ONE Record Server config
│   ├── application.yml               # Base server config
│   ├── application-kafka.yml         # Kafka producer config
│   ├── application-rdf4j.yml         # GraphDB config
│   ├── application-https.yml         # HTTPS/TLS config
│   ├── application-onerecord.yml     # ONE Record standard config
│   ├── application-airline-onerecord.yml    # Airline server config
│   └── application-forwarder-onerecord.yml  # Forwarder server config
└── servicediscovery/                 # Service Discovery config
    ├── application.yml               # Base Eureka config
    ├── application-prod.yml          # Production settings
    └── application-test.yml          # Test settings
```

---

## Verification & Testing

### Step 1: Basic Health Checks
```bash
# Check all services are running
docker compose ps

# Expected output - all services should show "Up" status
```

### Step 2: Service Health Endpoints
```bash
# Config Server
curl -f http://localhost:8888/actuator/health
# Expected: {"status":"UP"}

# Service Discovery
curl -f -u eureka:server http://localhost:8761/actuator/health
# Expected: {"status":"UP"}

# ONE Record Services
curl -f http://localhost:8081/actuator/health
curl -f http://localhost:8083/actuator/health
curl -f http://localhost:8085/actuator/health
# Expected: {"status":"UP"} for each
```

### Step 3: Service Discovery Verification
```bash
# Access Eureka Web UI
open http://localhost:8761/eurekawebui
# Login with: eureka / server

# Check registered services via API
curl -u eureka:server http://localhost:8761/eureka/apps
# Should show all services registered
```

### Step 4: Configuration Verification
```bash
# Test Config Server endpoints
curl http://localhost:8888/client/kafka
curl http://localhost:8888/server/kafka
curl http://localhost:8888/processor/rdf4j

# Each should return configuration JSON
```

### Step 5: GraphDB Verification
```bash
# List repositories
curl -u admin:admin http://localhost:7200/rest/repositories

# Check ontomesh repository exists
curl -u admin:admin http://localhost:7200/rest/repositories/ontomesh
```

### Step 6: Kafka Verification
```bash
# List Kafka topics
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Expected topics:
# - client-events
# - processor-events
# - server-events
```

### Step 7: Certificate Verification
```bash
# Verify SSL certificates
./verify-certs.sh

# Check certificate details
openssl x509 -in certs/tls.crt -text -noout
```

### Step 8: Automated Verification Scripts
```bash
# Run all verification scripts
./verify-certs.sh
./verify-graphdb.sh
./test-ontology.sh

# Check verification results
echo "Verification complete. Check output above for any errors."
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Port Conflicts
**Problem**: Service fails to start due to port already in use
```bash
# Error: Bind for 0.0.0.0:8085 failed: port is already allocated
```

**Solution**:
```bash
# Check what's using the port
lsof -i :8085

# Kill the process using the port
sudo lsof -ti:8085 | xargs kill -9

# Or change the port in docker-compose.yml
ports:
  - "8086:8080"  # Change 8085 to 8086
```

#### 2. Service Discovery Issues
**Problem**: Services not registering with Eureka
```bash
# Check Eureka logs
docker compose logs service-discovery

# Look for: "Cannot register with eureka server"
```

**Solution**:
```bash
# Verify Eureka credentials in .env file
EUREKA_USERNAME=eureka
EUREKA_PASSWORD=server

# Check service discovery URL
SERVICE_DISCOVERY_URL=http://eureka:server@service-discovery:8080/eureka

# Restart services
docker compose restart 1r-server 1r-client 1r-processor
```

#### 3. Configuration Issues
**Problem**: Services can't connect to Config Server
```bash
# Check Config Server logs
docker compose logs application-config-server

# Look for: "Could not resolve placeholder"
```

**Solution**:
```bash
# Verify config-repo is mounted correctly
docker compose exec application-config-server ls -la /app/config-repo

# Check Config Server credentials
CONFIG_USERNAME=configuser
CONFIG_PASSWORD=configpass

# Restart Config Server
docker compose restart application-config-server
```

#### 4. GraphDB Issues
**Problem**: Repository not created or ontology not imported
```bash
# Check GraphDB logs
docker compose logs graphdb

# Look for: "Repository 'ontomesh' not found"
```

**Solution**:
```bash
# Verify repository creation
curl -u admin:admin http://localhost:7200/rest/repositories

# If repository doesn't exist, recreate it
docker compose restart graphdb

# Wait for GraphDB to start, then check again
sleep 30
curl -u admin:admin http://localhost:7200/rest/repositories
```

#### 5. Kafka Issues
**Problem**: Services can't connect to Kafka
```bash
# Check Kafka logs
docker compose logs kafka

# Look for: "Connection refused"
```

**Solution**:
```bash
# Verify Kafka is running
docker compose ps kafka

# Check Kafka topics
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Restart Kafka if needed
docker compose restart kafka
```

#### 6. Memory Issues
**Problem**: Services failing due to insufficient memory
```bash
# Check Docker memory usage
docker stats

# Look for services showing high memory usage
```

**Solution**:
```bash
# Increase Docker memory limit (Docker Desktop)
# Settings > Resources > Memory: Increase to 8GB+

# Or reduce service memory limits in docker-compose.yml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m
```

### Debug Mode

#### Enable Debug Logging
```bash
# Add debug logging to specific service
docker compose exec 1r-server sh -c 'echo "logging.level.root=DEBUG" >> /app/application.yml'

# Restart service to apply changes
docker compose restart 1r-server

# Check debug logs
docker compose logs -f 1r-server
```

#### Verbose Docker Compose
```bash
# Start with verbose output
docker compose up -d --verbose

# Check detailed logs
docker compose logs --tail=100 -f
```

### Log Analysis

#### Common Log Patterns
```bash
# Service Discovery registration
docker compose logs | grep -i "registered"

# Configuration loading
docker compose logs | grep -i "config"

# GraphDB repository
docker compose logs | grep -i "repository"

# Kafka connectivity
docker compose logs | grep -i "kafka"

# Errors
docker compose logs | grep -i "error"
docker compose logs | grep -i "exception"
```

#### Service-Specific Logs
```bash
# Config Server logs
docker compose logs -f application-config-server

# Service Discovery logs
docker compose logs -f application-service-discovery

# ONE Record Server logs
docker compose logs -f 1r-server

# GraphDB logs
docker compose logs -f graphdb

# Kafka logs
docker compose logs -f kafka
```

---

## Maintenance & Operations

### Regular Maintenance Tasks

#### Daily Operations
```bash
# Check service status
docker compose ps

# Monitor logs for errors
docker compose logs --tail=50 | grep -i error

# Check resource usage
docker stats --no-stream
```

#### Weekly Operations
```bash
# Clean up old logs
docker system prune -f

# Update Docker images
docker compose pull

# Backup configuration
tar -czf config-backup-$(date +%Y%m%d).tar.gz config-repo/
```

#### Monthly Operations
```bash
# Full system cleanup
./cleanup.sh

# Rebuild with latest code
./start-fresh.sh

# Update SSL certificates
./generate-certs.sh
```

### Backup and Recovery

#### Backup Procedures
```bash
# Backup GraphDB data
docker exec graphdb tar -czf /tmp/graphdb-backup-$(date +%Y%m%d).tar.gz /opt/graphdb/data

# Backup configuration
tar -czf config-backup-$(date +%Y%m%d).tar.gz config-repo/

# Backup environment
cp .env .env.backup-$(date +%Y%m%d)
```

#### Recovery Procedures
```bash
# Restore GraphDB data
docker cp graphdb-backup-20241201.tar.gz graphdb:/tmp/
docker exec graphdb tar -xzf /tmp/graphdb-backup-20241201.tar.gz -C /

# Restore configuration
tar -xzf config-backup-20241201.tar.gz

# Restore environment
cp .env.backup-20241201 .env
```

### Monitoring and Alerting

#### Health Check Script
```bash
#!/bin/bash
# health-check.sh

SERVICES=("8888" "8761" "8081" "8083" "8085" "7200" "8443")

for port in "${SERVICES[@]}"; do
  if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
    echo "✅ Service on port $port is healthy"
  else
    echo "❌ Service on port $port is down"
  fi
done
```

#### Log Monitoring
```bash
# Monitor for errors
docker compose logs -f | grep -i error

# Monitor for specific patterns
docker compose logs -f | grep -i "out of memory"
docker compose logs -f | grep -i "connection refused"
```

### Performance Optimization

#### Memory Optimization
```yaml
# docker-compose.yml - Optimize memory usage
services:
  1r-server:
    environment:
      - JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

#### CPU Optimization
```yaml
# docker-compose.yml - CPU limits
services:
  1r-server:
    deploy:
      resources:
        limits:
          cpus: '2.0'
        reservations:
          cpus: '1.0'
```

### Security Hardening

#### Network Security
```yaml
# docker-compose.yml - Network isolation
networks:
  ontomesh-internal:
    internal: true
  ontomesh-external:
    driver: bridge
```

#### Access Control
```bash
# Change default passwords
# Update .env file with strong passwords
CONFIG_PASSWORD=your-strong-password
EUREKA_PASSWORD=your-strong-password
REPOSITORY_PASSWORD=your-strong-password
```

---

## Port Configuration

### Port Overview

#### Application Services
| Service | Internal Port | External Port | Protocol | Purpose |
|---------|---------------|---------------|----------|---------|
| Config Server | 8080 | 8888 | HTTP | Configuration management |
| Service Discovery | 8080 | 8761 | HTTP | Service registration |
| ONE Record Server | 8080 | 8085 | HTTP | Main API server |
| ONE Record Client | 8080 | 8081 | HTTP | Client service |
| Event Processor | 8080 | 8083 | HTTP | Event processing |

#### Infrastructure Services
| Service | Internal Port | External Port | Protocol | Purpose |
|---------|---------------|---------------|----------|---------|
| GraphDB | 7200 | 7200 | HTTP | RDF data storage |
| GraphDB Workbench | 7300 | 7300 | HTTP | Web interface |
| Keycloak | 8443 | 8443 | HTTPS | Identity management |
| PostgreSQL | 5432 | - | TCP | Keycloak database |
| Kafka | 9092 | 9092 | TCP | Event streaming |
| Zookeeper | 2181 | - | TCP | Kafka coordination |

### Docker Compose Configuration

#### Application Services (docker-compose.yml)
```yaml
services:
  application-config-server:
    ports:
      - "8888:8080"  # External:Internal
  
  application-service-discovery:
    ports:
      - "8761:8080"  # External:Internal
  
  1r-server:
    ports:
      - "8085:8080"  # External:Internal
  
  1r-client:
    ports:
      - "8081:8080"  # External:Internal
  
  1r-processor:
    ports:
      - "8083:8080"  # External:Internal
```

### Port Verification

#### Port Availability Check
```bash
# Check if ports are available
./check-ports.sh

# Manual port check
netstat -an | grep -E "(8081|8083|8085|8761|8888|7200|8443|9092)"

# On macOS/Linux
lsof -i :8081
lsof -i :8083
lsof -i :8085
lsof -i :8761
lsof -i :8888
lsof -i :7200
lsof -i :8443
lsof -i :9092
```

#### Port Conflict Resolution
```bash
# Find process using port
lsof -i :8085

# Kill process using port
sudo lsof -ti:8085 | xargs kill -9

# Alternative: Change port in docker-compose.yml
ports:
  - "8086:8080"  # Change external port
```

### Health Check Endpoints
```bash
# Config Server
curl -f http://localhost:8888/actuator/health

# Service Discovery
curl -f -u eureka:server http://localhost:8761/actuator/health

# ONE Record Services
curl -f http://localhost:8081/actuator/health
curl -f http://localhost:8083/actuator/health
curl -f http://localhost:8085/actuator/health

# GraphDB
curl -u admin:admin http://localhost:7200/rest/repositories

# Keycloak
curl -k https://localhost:8443/health
```

---

## Quick Reference

### Essential Commands
```bash
# Start everything
./start-fresh.sh

# Stop everything
docker compose down

# Check status
docker compose ps

# View logs
docker compose logs -f

# Restart service
docker compose restart service-name

# Clean up
./cleanup.sh
```



### Troubleshooting Commands
```bash
# Check ports
./check-ports.sh

# Verify certificates
./verify-certs.sh

# Verify GraphDB
./verify-graphdb.sh

# Test ontology
./test-ontology.sh

# Check service health
curl -f http://localhost:8085/actuator/health
```

### Available Scripts

#### Core Deployment Scripts
- **`start-fresh.sh`**: Complete fresh build and deployment
- **`docker-up.sh`**: Docker-only fresh build (faster)
- **`setup-env.sh`**: Interactive environment configuration

#### Utility Scripts
- **`generate-certs.sh`**: Generate SSL certificates
- **`check-ports.sh`**: Check port availability
- **`cleanup.sh`**: Complete system cleanup

#### Verification Scripts
- **`verify-certs.sh`**: Verify SSL certificate configuration
- **`verify-graphdb.sh`**: Verify GraphDB setup and connectivity
- **`test-ontology.sh`**: Test ontology configuration and data

---

## Additional Resources

- **Configuration Repository**: [../../config-repo/README.md](../../config-repo/README.md)
- **IATA ONE Record Standard**: [IATA ONE Record Documentation](https://www.iata.org/en/programs/cargo/one-record/)
- **Spring Boot Documentation**: [Spring Boot Reference](https://spring.io/projects/spring-boot)
- **GraphDB Documentation**: [GraphDB Documentation](https://graphdb.ontotext.com/documentation/)
- **Docker Documentation**: [Docker Compose Reference](https://docs.docker.com/compose/)

---

**Need Help?** If you encounter issues not covered in this guide, check the troubleshooting section or create an issue in the project repository.