# OntoMesh Deployment Scripts

This directory contains scripts to automatically build, deploy, and manage the OntoMesh application with fresh builds every time.

## Scripts Overview

### 1. `start-fresh.sh` - Complete Fresh Build and Deploy
**Use this when you want to rebuild everything from scratch**

This script:
- Stops all existing containers
- Removes old OntoMesh Docker images
- Builds all Maven projects (config-server, service-discovery, 1r-client, 1r-processor, 1r-server, ontomesh-core, ontomesh-webclient)
- Creates fresh Docker images with `--no-cache`
- Starts all services with `--force-recreate`
- Checks service health and shows logs for any issues

```bash
./start-fresh.sh
```

### 2. `docker-up.sh` - Docker Compose Fresh Start
**Use this when you only want to rebuild Docker images (assumes Maven projects are already built)**

This script:
- Stops all existing containers
- Removes old OntoMesh Docker images
- Builds fresh Docker images with `--no-cache`
- Starts all services with `--force-recreate`
- Checks service health

```bash
./docker-up.sh
```

### 3. `cleanup.sh` - Clean Up Old Resources
**Use this to remove old containers, images, and volumes**

This script:
- Stops and removes all containers
- Removes OntoMesh Docker images
- Removes dangling images
- Optionally removes unused volumes (will delete GraphDB and PostgreSQL data)
- Removes unused networks

```bash
./cleanup.sh
```

## Manual Commands

If you prefer to run commands manually:

### Fresh Build and Deploy
```bash
# Stop existing containers
docker compose down --remove-orphans

# Remove old images
docker images | grep -E "(application-config-server|application-service-discovery|1r-client|1r-processor|1r-server)" | awk '{print $3}' | xargs -r docker rmi -f

# Build and start with fresh builds
docker compose up --build --force-recreate -d
```

### Build Individual Services
```bash
# Build specific service
docker compose build --no-cache [service-name]

# Recreate specific service
docker compose up -d --force-recreate [service-name]
```

## Docker Compose Configuration

The `docker-compose.yml` has been configured with:
- `no_cache: true` for all build configurations
- Proper health checks with authentication
- Service dependencies and startup order
- Volume mounts for configuration and certificates

## Service Access Points

After deployment, services are available at:

- **Config Server**: http://localhost:8888
- **Service Discovery**: http://localhost:8761 (credentials: `eureka:server`)
- **1R Client**: http://localhost:8081
- **1R Processor**: http://localhost:8083
- **1R Server**: http://localhost:8085
- **Kafka**: localhost:9092
- **GraphDB**: http://localhost:7200
- **Keycloak**: https://localhost:8443

## Troubleshooting

### Check Service Status
```bash
docker compose ps
```

### View Service Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f [service-name]
```

### Restart Specific Service
```bash
docker compose restart [service-name]
```

### Force Rebuild Specific Service
```bash
docker compose build --no-cache [service-name]
docker compose up -d --force-recreate [service-name]
```

### Clean Everything and Start Fresh
```bash
./cleanup.sh
./start-fresh.sh
```

## Prerequisites

- Docker and Docker Compose installed
- Maven installed (for `start-fresh.sh`)
- Java 21+ installed (for Maven builds)
- Environment variables configured (see `env.example`)

## Notes

- The `start-fresh.sh` script builds all Maven projects, which can take several minutes
- The `docker-up.sh` script is faster as it only rebuilds Docker images
- Use `cleanup.sh` carefully as it can delete all data in GraphDB and PostgreSQL
- All scripts include colored output and status messages for better visibility
- Scripts will exit on any error to prevent partial deployments 