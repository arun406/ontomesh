# OntoMesh Port Configuration

## External Service URLs

### Application Services
| Service | External Port | Internal Port | URL | Description |
|---------|---------------|---------------|-----|-------------|
| Config Server | 8888 | 8080 | http://localhost:8888 | Spring Cloud Config Server |
| Service Discovery | 8761 | 8080 | http://localhost:8761 | Eureka Service Discovery |
| 1r-client | 8081 | 8080 | http://localhost:8081 | ONE Record Client Service |
| 1r-processor | 8083 | 8080 | http://localhost:8083 | Event Processing Service |
| 1r-server | 8085 | 8080 | http://localhost:8085 | Main ONE Record Server |

### Infrastructure Services
| Service | External Port | Internal Port | URL | Description |
|---------|---------------|---------------|-----|-------------|
| Kafka | 9092 | 9092 | localhost:9092 | Apache Kafka Broker |
| Keycloak | 8443 | 8443 | https://localhost:8443 | Identity & Access Management |
| GraphDB REST | 7200 | 7200 | http://localhost:7200 | GraphDB REST API |
| GraphDB Workbench | 7300 | 7300 | http://localhost:7300 | GraphDB Web Interface |

### Internal Services (No External Port)
| Service | Internal Port | Description |
|---------|---------------|-------------|
| PostgreSQL | 5432 | Keycloak Database |

## Service Dependencies

### Startup Order
1. **Config Server** (8888) - Centralized configuration
2. **Service Discovery** (8761) - Service registration
3. **Infrastructure Services**:
   - **PostgreSQL** (5432) - Database
   - **Kafka** (9092) - Message broker
   - **GraphDB** (7200/7300) - RDF storage
   - **Keycloak** (8443) - Authentication
4. **Application Services**:
   - **1r-server** (8085) - Main API
   - **1r-client** (8081) - Client service
   - **1r-processor** (8083) - Event processor

## Health Check Endpoints

### Application Services
- Config Server: `http://localhost:8888/actuator/health`
- Service Discovery: `http://localhost:8761/actuator/health`
- 1r-client: `http://localhost:8081/actuator/health`
- 1r-processor: `http://localhost:8083/actuator/health`
- 1r-server: `http://localhost:8085/actuator/health`

### Infrastructure Services
- Keycloak: `https://localhost:8443/health/ready`
- GraphDB: `http://localhost:7200/rest/repositories`
- Kafka: Internal health check via `kafka-topics.sh`

## Network Configuration

### Docker Network
- **Network Name**: `ontomesh-network`
- **Driver**: bridge
- **Internal Communication**: All services can communicate using container names

### External Access
- **Host**: localhost (127.0.0.1)
- **Protocols**: HTTP (8081, 8083, 8085, 8761, 8888, 7200, 7300), HTTPS (8443), TCP (9092)

## Security Considerations

### HTTPS Services
- **Keycloak**: HTTPS only (8443)
- **Self-signed certificates**: Generated automatically for development

### HTTP Services
- **All other services**: HTTP for development
- **Production**: Should use HTTPS with proper certificates

## Troubleshooting

### Port Conflicts
If you encounter port conflicts, you can modify the external ports in `docker-compose.yml`:

```yaml
ports:
  - "NEW_PORT:8080"  # Change NEW_PORT to available port
```

### Common Issues
1. **Port already in use**: Check if another service is using the port
2. **Cannot connect**: Verify service is running and healthy
3. **SSL errors**: Check certificate configuration for Keycloak

### Verification Commands
```bash
# Check all running containers and ports
docker-compose ps

# Check specific service logs
docker-compose logs SERVICE_NAME

# Test service connectivity
curl http://localhost:PORT/actuator/health
``` 