# Config Server searchLocations Configuration Alignment

## Overview
This document describes the alignment of the Spring Cloud Config Server `searchLocations` configuration across the OntoMesh deployment components.

## Problem Statement

### Original Issues
1. **Hardcoded Path**: Config Server used hardcoded localhost path `file:///Users/arun/source/ontomesh/config-repo`
2. **Environment Variable Disabled**: `SEARCH_LOCATIONS` was commented out in docker-compose.yml
3. **Container Path Mismatch**: Docker mount path `/app/config-repo` didn't match application configuration
4. **Documentation Gap**: `SEARCH_LOCATIONS` not documented in environment variables

### Impact
- Config Server couldn't find configuration files in containerized environment
- Inconsistent configuration between development and deployment
- Environment variable not configurable for different deployment scenarios

## Solution Implementation

### 1. Config Server Application Configuration
**File**: `ontomesh-configserver/src/main/resources/application.yml`

**Before**:
```yaml
searchLocations: ${SEARCH_LOCATIONS:file:///Users/arun/source/ontomesh/config-repo}
```

**After**:
```yaml
searchLocations: ${SEARCH_LOCATIONS:file:/app/config-repo}
```

**Changes**:
- ✅ Removed hardcoded localhost path
- ✅ Used container-relative path `/app/config-repo`
- ✅ Maintained environment variable configurability

### 2. Docker Compose Configuration
**File**: `deployment/application/docker-compose.yml`

**Before**:
```yaml
environment:
  - CONFIG_SERVER_PORT=${CONFIG_SERVER_PORT}
  - CONFIG_USERNAME=${CONFIG_USERNAME}
  - CONFIG_PASSWORD=${CONFIG_PASSWORD}
  #      - SEARCH_LOCATIONS=${SEARCH_LOCATIONS}
  - DISCOVERY_CLIENT_ENABLED=${DISCOVERY_CLIENT_ENABLED}
volumes:
  - ./../../config-repo:/app/config-repo
```

**After**:
```yaml
environment:
  - CONFIG_SERVER_PORT=${CONFIG_SERVER_PORT}
  - CONFIG_USERNAME=${CONFIG_USERNAME}
  - CONFIG_PASSWORD=${CONFIG_PASSWORD}
  - SEARCH_LOCATIONS=${SEARCH_LOCATIONS}
  - DISCOVERY_CLIENT_ENABLED=${DISCOVERY_CLIENT_ENABLED}
volumes:
  - ./../../config-repo:/app/config-repo
```

**Changes**:
- ✅ Enabled `SEARCH_LOCATIONS` environment variable
- ✅ Maintained volume mount configuration
- ✅ Aligned with application configuration

### 3. Environment Variables
**File**: `deployment/application/env.example`

**Added**:
```bash
SEARCH_LOCATIONS=file:/app/config-repo
```

**File**: `deployment/application/.env`

**Updated**: Copied from env.example with correct SEARCH_LOCATIONS value

### 4. Documentation Updates

#### Config Repository README
**File**: `config-repo/README.md`

**Added**:
```markdown
| `SEARCH_LOCATIONS` | `file:/app/config-repo` | Path to configuration repository |
```

#### Validation Script
**File**: `config-repo/validate-config.sh`

**Added**:
- Config Server searchLocations validation
- Docker Compose SEARCH_LOCATIONS validation
- Environment variable documentation validation

## Configuration Flow

### Deployment Flow
1. **Docker Compose** mounts `./../../config-repo` to `/app/config-repo` in container
2. **Environment Variable** `SEARCH_LOCATIONS=file:/app/config-repo` is set
3. **Config Server** reads `SEARCH_LOCATIONS` and uses `/app/config-repo` as search location
4. **Configuration Files** are served from the mounted volume

### Environment Variable Resolution
```bash
# Default value in application.yml
searchLocations: ${SEARCH_LOCATIONS:file:/app/config-repo}

# Environment variable in docker-compose.yml
- SEARCH_LOCATIONS=${SEARCH_LOCATIONS}

# Value in .env file
SEARCH_LOCATIONS=file:/app/config-repo
```

## Validation

### Automated Validation
The validation script checks:
- ✅ Config Server uses correct container path
- ✅ SEARCH_LOCATIONS is configured in docker-compose.yml
- ✅ SEARCH_LOCATIONS is documented in env.example

### Manual Validation
```bash
# Check Config Server configuration
curl http://localhost:8888/actuator/health

# Test configuration endpoints
curl http://localhost:8888/client/dev
curl http://localhost:8888/server/kafka
curl http://localhost:8888/processor/rdf4j

# Verify container mount
docker-compose exec config-server ls -la /app/config-repo
```

## Benefits

### Consistency
- ✅ Same configuration works in development and production
- ✅ Environment variable allows customization for different deployments
- ✅ Container paths align with Docker volume mounts

### Flexibility
- ✅ Easy to change search location via environment variable
- ✅ Supports different deployment scenarios (local, cloud, etc.)
- ✅ Maintains backward compatibility

### Maintainability
- ✅ No hardcoded paths in configuration
- ✅ Clear documentation of environment variables
- ✅ Automated validation prevents misconfiguration

## Troubleshooting

### Common Issues

#### Config Server Can't Find Configuration
```bash
# Check if volume is mounted correctly
docker-compose exec config-server ls -la /app/config-repo

# Check environment variable
docker-compose exec config-server env | grep SEARCH_LOCATIONS

# Check Config Server logs
docker-compose logs config-server
```

#### Configuration Not Loading
```bash
# Test Config Server health
curl http://localhost:8888/actuator/health

# Test specific configuration
curl http://localhost:8888/client/dev

# Check file permissions
docker-compose exec config-server ls -la /app/config-repo/
```

### Debugging Commands
```bash
# Validate configuration
cd config-repo && ./validate-config.sh

# Check all environment variables
docker-compose exec config-server env

# Test file access
docker-compose exec config-server cat /app/config-repo/application.yml
```

## Future Considerations

### Production Deployments
- Consider using external configuration repositories (Git, Vault)
- Implement configuration encryption for sensitive data
- Set up configuration refresh mechanisms

### Scaling
- Ensure configuration repository is accessible to all Config Server instances
- Consider using shared storage for configuration files
- Implement configuration caching for performance

### Security
- Review file permissions on configuration repository
- Consider using encrypted configuration values
- Implement access controls for configuration endpoints 