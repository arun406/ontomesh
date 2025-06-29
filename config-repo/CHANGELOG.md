# Configuration Repository Changelog

## [2024-01-XX] - Docker Compose Alignment Update

### Summary
Updated all configuration files in the config-repo to align with the simplified single-node docker-compose deployment.

### Changes Made

#### Kafka Configuration Updates
- **Files Updated**: 
  - `client/application-kafka.yml`
  - `processor/application-kafka.yml`
  - `server/application-kafka.yml`

- **Changes**:
  - Updated `KAFKA_BOOTSTRAP_SERVERS` default from `localhost:29092,localhost:39092,localhost:49092` to `kafka:9092`
  - Aligned with single-node Kafka setup in docker-compose.yml
  - Removed references to multi-broker cluster configuration

#### GraphDB/RDF4J Configuration Updates
- **Files Updated**:
  - `processor/application-rdf4j.yml`
  - `server/application-rdf4j.yml`

- **Changes**:
  - Updated `REPOSITORY_URL` default from `http://GGKMAC23066.local:7201` to `http://graphdb:7200`
  - Aligned with single-node GraphDB setup in docker-compose.yml
  - Removed localhost references

#### Service Discovery Configuration Updates
- **Files Updated**:
  - `application.yml`
  - `servicediscovery/application.yml`

- **Changes**:
  - Updated service discovery URL to use container name `service-discovery:8080`
  - Standardized port configuration to use internal container ports
  - Fixed Eureka configuration to use consistent environment variables

### Environment Variables Alignment

#### Updated Default Values
| Variable | Old Default | New Default | Description |
|----------|-------------|-------------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:29092,localhost:39092,localhost:49092` | `kafka:9092` | Single-node Kafka broker |
| `REPOSITORY_URL` | `http://GGKMAC23066.local:7201` | `http://graphdb:7200` | Single-node GraphDB |
| `SERVICE_DISCOVERY_URL` | `http://eureka:server@localhost:8761/eureka` | `http://eureka:server@service-discovery:8080/eureka` | Container-based service discovery |

#### Consistent Environment Variables
All services now use consistent environment variable patterns:
- `ONERECORD_CLIENT_PORT`: Client service port
- `ONERECORD_PROCESSOR_PORT`: Processor service port  
- `ONERECORD_SERVER_PORT`: Server service port
- `SERVICE_DISCOVERY_SERVER_PORT`: Service discovery port
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `REPOSITORY_URL`: GraphDB repository URL
- `REPOSITORY_NAME`: GraphDB repository name
- `REPOSITORY_USERNAME`: GraphDB username
- `REPOSITORY_PASSWORD`: GraphDB password

### Configuration Profiles

#### Available Profiles
- **Client**: `dev`, `kafka`, `airline-clients`, `forwarder-clients`
- **Processor**: `dev`, `kafka`, `rdf4j`, `onerecord`
- **Server**: `dev`, `kafka`, `rdf4j`, `https`, `onerecord`, `airline-onerecord`, `forwarder-onerecord`
- **Service Discovery**: `dev`, `test`, `prod`

### Validation

#### Pre-Update Issues
- ❌ Multi-broker Kafka configuration not aligned with single-node setup
- ❌ GraphDB localhost references not aligned with container setup
- ❌ Inconsistent service discovery URLs
- ❌ Mixed port configurations (external vs internal)

#### Post-Update Validation
- ✅ All Kafka configurations use single-node setup (`kafka:9092`)
- ✅ All GraphDB configurations use correct URL (`graphdb:7200`)
- ✅ Service discovery uses correct container name (`service-discovery:8080`)
- ✅ Consistent environment variable usage across all services
- ✅ All configuration profiles properly defined

### Testing

#### Configuration Endpoints
After deployment, test the following endpoints:
```bash
# Config Server health
curl http://localhost:8888/actuator/health

# Service configurations
curl http://localhost:8888/client/dev
curl http://localhost:8888/server/kafka
curl http://localhost:8888/processor/rdf4j
curl http://localhost:8888/servicediscovery/dev
```

#### Validation Script
Run the validation script to verify configuration:
```bash
cd config-repo
./validate-config.sh
```

### Impact

#### Services Affected
- **1r-client**: Updated Kafka configuration for single-node setup
- **1r-processor**: Updated Kafka and GraphDB configurations
- **1r-server**: Updated Kafka and GraphDB configurations
- **service-discovery**: Updated Eureka configuration

#### Deployment Impact
- ✅ Simplified deployment with single-node services
- ✅ Consistent container-based networking
- ✅ Proper environment variable resolution
- ✅ Aligned with docker-compose.yml configuration

### Migration Notes

#### For Existing Deployments
1. Update environment variables to use new defaults
2. Ensure docker-compose.yml uses the updated configuration
3. Restart services to pick up new configuration
4. Verify connectivity between services

#### For New Deployments
1. Use the provided .env file with correct port mappings
2. Follow the deployment README for setup instructions
3. Run validation scripts to verify configuration
4. Test all service endpoints

### Future Considerations

#### Scalability
- Current single-node setup is suitable for development and small deployments
- For production scaling, consider:
  - Multi-node Kafka cluster
  - GraphDB cluster configuration
  - Load balancer setup
  - External service discovery

#### Security
- Review and update security configurations for production
- Consider external certificate management
- Implement proper authentication and authorization
- Audit environment variable usage

#### Monitoring
- Implement proper logging and monitoring
- Set up health checks for all services
- Configure metrics collection
- Establish alerting for service failures

### Service Discovery
- **Updated**: Eureka configuration to use consistent environment variables
- **Fixed**: Service discovery URL to use container name (`service-discovery:8080`)
- **Standardized**: Port configuration across all services

### Config Server searchLocations Alignment
- **Updated**: Config Server application.yml to use container path (`file:/app/config-repo`)
- **Fixed**: Removed hardcoded localhost path (`file:///Users/arun/source/ontomesh/config-repo`)
- **Enabled**: SEARCH_LOCATIONS environment variable in docker-compose.yml
- **Documented**: SEARCH_LOCATIONS in env.example and documentation
- **Validated**: Added searchLocations validation to configuration validation script 