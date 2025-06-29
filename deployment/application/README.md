# OntoMesh Application Deployment

This directory contains the Docker Compose configuration for deploying the complete OntoMesh ONE Record implementation.

## Prerequisites

- Docker and Docker Compose installed
- At least 6GB RAM available for Docker
- Ports 8888, 8761, 8081, 8083, 8085, 7200, 7300, 8443, 9092 available

## Quick Start

1. **Generate SSL certificates:**
   ```bash
   ./generate-certs.sh
   ```

2. **Check port availability:**
   ```bash
   ./check-ports.sh
   ```

3. **Set up environment variables:**
   ```bash
   # Interactive setup (recommended for first time)
   ./setup-env.sh
   
   # Non-interactive setup with defaults
   ./setup-env.sh -n
   
   # Configure only specific sections
   ./setup-env.sh -s service-discovery
   ./setup-env.sh -s all -n
   ```

4. **Review and modify the .env file** with your specific configuration values.

5. **Start the complete stack:**
   ```bash
   docker-compose up -d
   ```

6. **Monitor the startup:**
   ```bash
   docker-compose logs -f
   ```

7. **Verify certificate setup:**
   ```bash
   ./verify-certs.sh
   ```

8. **Verify GraphDB setup:**
   ```bash
   ./verify-graphdb.sh
   ```

9. **Test IATA ontology:**
   ```bash
   ./test-ontology.sh
   ```

## Environment Setup Script

The `setup-env.sh` script provides a comprehensive way to configure all environment variables for the OntoMesh deployment.

### Features
- **Interactive Mode**: Guided setup with prompts for each variable
- **Non-Interactive Mode**: Use default values without prompts (`-n` flag)
- **Selective Configuration**: Configure only specific sections (`-s` flag)
- **Force Overwrite**: Overwrite existing .env file (`-f` flag)
- **Section Listing**: Show available configuration sections (`--show-sections`)

### Available Configuration Sections
- **config-server**: Config Server settings (port, credentials, search locations)
- **service-discovery**: Service Discovery settings (Eureka credentials, profiles, host, port)
- **client**: ONE Record Client settings (profiles, port)
- **processor**: ONE Record Processor settings (profiles, port)
- **server**: ONE Record Server settings (profiles, port, URL, IRI scheme)
- **graphdb**: GraphDB settings (URL, repository, credentials)
- **kafka**: Kafka settings (bootstrap servers, client/group IDs)
- **keycloak**: Keycloak settings (issuer URI, database credentials)

### Usage Examples
```bash
# Show available sections
./setup-env.sh --show-sections

# Interactive setup (recommended for first time)
./setup-env.sh

# Non-interactive setup with all defaults
./setup-env.sh -n

# Configure only service discovery interactively
./setup-env.sh -s service-discovery

# Configure all sections with defaults
./setup-env.sh -s all -n

# Force overwrite existing .env file
./setup-env.sh -f

# Get help
./setup-env.sh -h
```

### Default Values
The script includes sensible defaults for all environment variables:
- **Config Server**: `configuser`/`configpass`
- **Eureka Service Discovery**: `eureka`/`server`
- **Service Discovery Host**: `service-discovery` (container name)
- **Service Discovery Port**: `8080` (internal container port)
- **GraphDB**: `admin`/`admin`
- **PostgreSQL**: `keycloak`/`keycloak`
- **Kafka**: Single-node setup (`kafka:9092`)
- **Ports**: Standard Spring Boot ports (8080)

### Customization
After running the setup script, you can:
1. Edit the `.env` file directly for fine-tuning
2. Run the script again to update specific sections
3. Update corresponding config-repo files if needed

## Services Overview

### Core Application Services
- **Config Server** (Port 8888): Centralized configuration management
- **Service Discovery** (Port 8761): Eureka-based service discovery
- **1r-client** (Port 8081): ONE Record client service
- **1r-processor** (Port 8083): Event processing service
- **1r-server** (Port 8085): Main ONE Record server

### Infrastructure Services
- **GraphDB** (Ports 7200, 7300): Single-node RDF storage and SPARQL endpoint
- **Kafka** (Port 9092): Single-node event streaming platform
- **Keycloak** (Port 8443): Identity and access management
- **PostgreSQL**: Database for Keycloak

## Configuration Management

### Spring Cloud Config Server
The deployment uses Spring Cloud Config Server for centralized configuration management. All microservices retrieve their configuration from the Config Server at startup.

### Configuration Repository
- **Location**: `../../config-repo/` (mounted to Config Server)
- **Documentation**: [Config Repository README](../../config-repo/README.md)
- **Validation**: [Configuration Validation Script](../../config-repo/validate-config.sh)
- **Changelog**: [Configuration Changes](../../config-repo/CHANGELOG.md)

### Configuration Structure
```
config-repo/
├── application.yml                    # Global configuration
├── client/                           # 1r-client service configuration
├── processor/                        # 1r-processor service configuration
├── server/                           # 1r-server service configuration
└── servicediscovery/                 # Service Discovery configuration
```

### Environment Variables
The Config Server uses environment variables from the `.env` file to configure:
- **Kafka**: Single-node setup (`kafka:9092`)
- **GraphDB**: Single-node setup (`http://graphdb:7200`)
- **Service Discovery**: Container-based networking with Eureka credentials (`EUREKA_USERNAME`/`EUREKA_PASSWORD`)
- **Security**: OAuth2 and certificate configuration
- **Config Server**: Authentication credentials (`CONFIG_USERNAME`/`CONFIG_PASSWORD`)

### Configuration Validation
Before deployment, validate the configuration:
```bash
cd ../../config-repo
./validate-config.sh
```

### Configuration Endpoints
After deployment, test configuration endpoints:
```bash
# Config Server health
curl http://localhost:8888/actuator/health

# Service configurations
curl http://localhost:8888/client/dev
curl http://localhost:8888/server/kafka
curl http://localhost:8888/processor/rdf4j
```

> **Note**: For detailed configuration documentation, see [Config Repository README](../../config-repo/README.md)

## Service URLs

- **Config Server**: http://localhost:8888
- **Service Discovery**: http://localhost:8761
- **Service Discovery Web UI**: http://localhost:8761/eurekawebui (credentials: `eureka`/`server`)
- **ONE Record Client**: http://localhost:8081
- **ONE Record Processor**: http://localhost:8083
- **ONE Record Server**: http://localhost:8085
- **GraphDB Workbench**: http://localhost:7300
- **GraphDB REST API**: http://localhost:7200
- **Kafka**: localhost:9092
- **Keycloak Admin**: https://localhost:8443

> **Note**: For detailed port configuration and troubleshooting, see [ports.md](ports.md)

## Certificate Management

### Overview
The deployment uses self-signed SSL certificates for Keycloak HTTPS access. All application containers automatically import these certificates into their Java truststores at startup.

### Certificate Files
- `certs/tls.crt`: Public certificate
- `certs/tls.key`: Private key
- `init-certs.sh`: Certificate initialization script
- `generate-certs.sh`: Certificate generation script
- `verify-certs.sh`: Certificate verification script

### Ontology Files
- `graphdb/IATA-1R-DM-Ontology.ttl`: IATA ONE Record ontology (515KB)
- `graphdb/repo-ontomesh.ttl`: GraphDB repository configuration
- `graphdb/init-repo.sh`: Repository and ontology initialization script

### Certificate Workflow
1. **Generation**: `./generate-certs.sh` creates self-signed certificates
2. **Mounting**: Certificates are mounted to all application containers
3. **Import**: Each container imports certificates into Java truststore at startup
4. **Verification**: `./verify-certs.sh` validates the setup

### Certificate Scripts
- **`generate-certs.sh`**: Creates self-signed certificates for development
- **`verify-certs.sh`**: Validates certificate configuration and connectivity
- **`init-certs.sh`**: Imports certificates into Java truststore (used by containers)

### Production Considerations
For production deployments:
1. Replace self-signed certificates with CA-signed certificates
2. Update certificate paths in docker-compose.yml
3. Ensure proper certificate renewal procedures
4. Consider using external certificate management (e.g., Let's Encrypt)

## Architecture Improvements

### Simplified Kafka Setup
- **Single Node**: Replaced complex 3-controller + 3-broker cluster with single node
- **Combined Roles**: Controller and broker roles in one container
- **Health Checks**: Proper health monitoring for Kafka availability
- **Auto Topics**: Automatic topic creation enabled

### Simplified GraphDB Setup
- **Single Node**: Clean single-node GraphDB configuration
- **Memory Optimization**: Configured with 2GB heap size for better performance
- **Health Checks**: Proper health monitoring with wget
- **Repository Setup**: Automatic repository creation with initialization script
- **IATA Ontology**: Automatic import of IATA ONE Record ontology (515KB, ~10K+ triples)
- **No Cluster Complexity**: Removed all cluster-related configurations

### Enhanced Keycloak Configuration
- **Health Checks**: Proper health monitoring with curl
- **Database Dependencies**: Waits for PostgreSQL to be healthy
- **HTTPS Only**: Secure configuration with proper certificate handling
- **Metrics**: Health and metrics endpoints enabled

### Improved Service Dependencies
- **Health-based Dependencies**: Services wait for dependencies to be healthy
- **Proper Startup Order**: Config Server → Service Discovery → Infrastructure → Applications
- **Robust Health Checks**: All critical services have health monitoring

### Certificate Integration
- **Automatic Import**: All application containers import certificates at startup
- **Runtime Configuration**: Certificates are imported dynamically, not baked into images
- **Verification**: Comprehensive certificate validation and testing
- **Flexibility**: Easy to update certificates without rebuilding images

## Troubleshooting

### Certificate Issues
If you encounter SSL/certificate errors:

1. **Regenerate certificates:**
   ```bash
   ./generate-certs.sh
   ```

2. **Verify certificate setup:**
   ```bash
   ./verify-certs.sh
   ```

3. **Check certificate import logs:**
   ```bash
   docker-compose logs 1r-server | grep -i cert
   ```

4. **Test Keycloak connectivity:**
   ```bash
   curl -k https://localhost:8443/health/ready
   ```

### GraphDB Issues
If GraphDB is not starting properly:

1. **Check logs:**
   ```bash
   docker-compose logs graphdb
   docker-compose logs graph-db-setup
   ```

2. **Verify repository creation:**
   ```bash
   curl http://localhost:7200/rest/repositories
   ```

3. **Check memory configuration:**
   ```bash
   docker-compose exec graphdb env | grep GRAPHDB_HEAP_SIZE
   ```

4. **Verify single-node setup:**
   ```bash
   ./verify-graphdb.sh
   ```

5. **Check IATA ontology import:**
   ```bash
   curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
     --data-urlencode "query=ASK { <https://onerecord.iata.org/ns/cargo> a <http://www.w3.org/2002/07/owl#Ontology> }" \
     -H "Accept: application/sparql-results+json"
   ```

6. **Restart GraphDB:**
   ```bash
   docker-compose restart graphdb graph-db-setup
   ```

### Kafka Issues
If Kafka is not starting:

1. **Check logs:**
   ```bash
   docker-compose logs kafka
   ```

2. **Verify Kafka connectivity:**
   ```bash
   docker-compose exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
   ```

3. **Test from host:**
   ```bash
   docker-compose exec kafka kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test
   ```

### Keycloak Issues
If Keycloak is not accessible:

1. **Check certificate setup:**
   ```bash
   ls -la certs/
   ```

2. **Generate certificates if missing:**
   ```bash
   ./generate-certs.sh
   ```

3. **Check health:**
   ```bash
   curl -k https://localhost:8443/health/ready
   ```

4. **Check logs:**
   ```bash
   docker-compose logs keycloak
   ```

### PostgreSQL Issues
If PostgreSQL is not starting:

1. **Check logs:**
   ```bash
   docker-compose logs postgres
   ```

2. **Verify connectivity:**
   ```bash
   docker-compose exec postgres pg_isready -U keycloak
   ```

### Application SSL Issues
If applications cannot connect to Keycloak:

1. **Check certificate import:**
   ```bash
   docker-compose exec 1r-server keytool -list -keystore /usr/lib/jvm/java-21-amazon-corretto/lib/security/cacerts -storepass changeit -alias keycloak
   ```

2. **Restart applications:**
   ```bash
   docker-compose restart 1r-server 1r-client 1r-processor
   ```

3. **Check application logs:**
   ```bash
   docker-compose logs 1r-server | grep -i ssl
   ```

### Configuration Issues
If services are not starting or have configuration problems:

1. **Validate configuration repository:**
   ```bash
   cd ../../config-repo
   ./validate-config.sh
   ```

2. **Check Config Server health:**
   ```bash
   curl http://localhost:8888/actuator/health
   ```

3. **Test configuration endpoints:**
   ```bash
   curl http://localhost:8888/client/dev
   curl http://localhost:8888/server/kafka
   curl http://localhost:8888/processor/rdf4j
   ```

4. **Check Config Server logs:**
   ```bash
   docker-compose logs config-server
   ```

5. **Verify environment variables:**
   ```bash
   docker-compose exec config-server env | grep -E "(KAFKA|REPOSITORY|SERVICE_DISCOVERY)"
   ```

6. **Review configuration documentation:**
   - [Config Repository README](../../config-repo/README.md)
   - [Configuration Validation](../../config-repo/validate-config.sh)
   - [Configuration Changes](../../config-repo/CHANGELOG.md)

### Service Discovery Issues
If Service Discovery (Eureka) is not working properly:

1. **Check Service Discovery health:**
   ```bash
   curl http://localhost:8761/actuator/health
   ```

2. **Access Service Discovery Web UI:**
   ```bash
   # Open in browser: http://localhost:8761/eurekawebui
   # Default credentials: eureka/server
   ```

3. **Check Service Discovery logs:**
   ```bash
   docker-compose logs service-discovery
   ```

4. **Verify Eureka credentials:**
   ```bash
   docker-compose exec service-discovery env | grep EUREKA
   ```

5. **Test Eureka API:**
   ```bash
   curl -u eureka:server http://localhost:8761/eureka/apps
   ```

6. **Reconfigure Service Discovery:**
   ```bash
   ./setup-env.sh -s service-discovery
   ```

7. **Restart Service Discovery:**
   ```bash
   docker-compose restart service-discovery
   ```

## Stopping the Stack

```bash
docker-compose down
```

To remove all data volumes:
```bash
docker-compose down -v
```

## Performance Notes

- **Single Node Kafka**: Suitable for development and testing. For production, consider multi-node setup
- **Single Node GraphDB**: Optimized for development with 2GB heap size. For production, consider cluster setup for high availability
- **Resource Usage**: Reduced from 6 Kafka containers to 1, saving significant resources
- **Startup Time**: Faster startup with simplified architecture
- **Monitoring**: All services have health checks for better monitoring
- **Certificate Management**: Runtime certificate import allows for easy updates without rebuilding images