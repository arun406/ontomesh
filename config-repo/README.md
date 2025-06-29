# OntoMesh Configuration Repository

This directory contains the configuration files for the Spring Cloud Config Server used by the OntoMesh microservices.

## Configuration Structure

```
config-repo/
├── application.yml                    # Global configuration
├── client/                           # 1r-client service configuration
│   ├── application.yml               # Client base configuration
│   ├── application-kafka.yml         # Kafka configuration
│   ├── application-airline-clients.yml
│   └── application-forwarder-clients.yml
├── processor/                        # 1r-processor service configuration
│   ├── application.yml               # Processor base configuration
│   ├── application-kafka.yml         # Kafka configuration
│   ├── application-onerecord.yml     # ONE Record configuration
│   └── application-rdf4j.yml         # GraphDB/RDF4J configuration
├── server/                           # 1r-server service configuration
│   ├── application.yml               # Server base configuration
│   ├── application-kafka.yml         # Kafka configuration
│   ├── application-rdf4j.yml         # GraphDB/RDF4J configuration
│   ├── application-https.yml         # HTTPS configuration
│   ├── application-onerecord.yml     # ONE Record configuration
│   ├── application-airline-onerecord.yml
│   └── application-forwarder-onerecord.yml
└── servicediscovery/                 # Service Discovery configuration
    ├── application.yml               # Eureka server configuration
    ├── application-prod.yml
    └── application-test.yml
```

## Environment Variables

### Global Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `ONTOMESH_HOME` | - | Base directory for logs and data |
| `SERVICE_DISCOVERY_URL` | `http://eureka:server@service-discovery:8080/eureka` | Eureka service discovery URL |

### Config Server
| Variable | Default | Description |
|----------|---------|-------------|
| `CONFIG_SERVER_PORT` | `8080` | Config server internal port |
| `CONFIG_USERNAME` | `configuser` | Config server username |
| `CONFIG_PASSWORD` | `configpass` | Config server password |
| `SEARCH_LOCATIONS` | `file:/app/config-repo` | Path to configuration repository |
| `DISCOVERY_CLIENT_ENABLED` | `true` | Enable service discovery client |

### Service Discovery
| Variable | Default | Description |
|----------|---------|-------------|
| `SERVICE_DISCOVERY_SERVER_PORT` | `8080` | Eureka server internal port |
| `SERVICE_DISCOVERY_PROFILE` | `dev` | Service discovery profile |
| `EUREKA_USERNAME` | `eureka` | Eureka server username |
| `EUREKA_PASSWORD` | `server` | Eureka server password |
| `SERVICE_DISCOVERY_HOST` | `localhost` | Eureka server hostname |

### ONE Record Client
| Variable | Default | Description |
|----------|---------|-------------|
| `ONERECORD_CLIENT_PORT` | `9095` | Client service internal port |
| `ONERECORD_CLIENT_PROFILES` | `dev` | Client service profiles |
| `LOGISTICS_EVENTS_NOTIFICATION_TOPIC` | `notifications` | Kafka topic for notifications |

### ONE Record Processor
| Variable | Default | Description |
|----------|---------|-------------|
| `ONERECORD_PROCESSOR_PORT` | `8080` | Processor service internal port |
| `ONERECORD_PROCESSOR_PROFILES` | `dev` | Processor service profiles |
| `LOGISTICS_EVENT_TOPIC` | `logistics-events` | Kafka topic for logistics events |
| `LOGISTICS_EVENTS_NOTIFICATION_TOPIC` | `notifications` | Kafka topic for notifications |

### ONE Record Server
| Variable | Default | Description |
|----------|---------|-------------|
| `ONERECORD_SERVER_PORT` | `8080` | Server service internal port |
| `ONERECORD_SERVER_PROFILES` | `dev` | Server service profiles |
| `ONERECORD_SERVER_URL` | - | Server external URL |
| `IRI_SCHEME` | `https` | IRI scheme for resource identifiers |
| `ISSUER_URI` | `https://keycloak.ontomesh.io:8443/realms/ontomesh` | OAuth2 issuer URI |

### Kafka Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Kafka broker addresses |
| `KAFKA_CLIENT_ID` | `client-01` | Kafka client ID |
| `KAFKA_GROUP_ID` | `group-01` | Kafka consumer group ID |

### GraphDB/RDF4J Configuration
| Variable | Default | Description |
|----------|---------|-------------|
| `REPOSITORY_URL` | `http://graphdb:7200` | GraphDB REST API URL |
| `REPOSITORY_NAME` | `ontomesh` | GraphDB repository name |
| `REPOSITORY_USERNAME` | `admin` | GraphDB username |
| `REPOSITORY_PASSWORD` | `admin` | GraphDB password |

## Configuration Profiles

### Client Profiles
- `kafka`: Kafka-enabled configuration
- `airline-clients`: Airline-specific client configuration
- `forwarder-clients`: Forwarder-specific client configuration

### Server Profiles
- `kafka`: Kafka-enabled configuration
- `rdf4j`: RDF4J/GraphDB-enabled configuration
- `https`: HTTPS-enabled configuration
- `onerecord`: ONE Record-specific configuration
- `airline-onerecord`: Airline-specific ONE Record configuration
- `forwarder-onerecord`: Forwarder-specific ONE Record configuration

### Processor Profiles
- `kafka`: Kafka-enabled configuration
- `rdf4j`: RDF4J/GraphDB-enabled configuration
- `onerecord`: ONE Record-specific configuration
- `airline-onerecord`: Airline-specific ONE Record processor configuration

**Note**: Base configuration is provided by the main `application.yml` files in each service directory. Profiles are used to enable additional features or environment-specific settings.

## Recent Updates

### Kafka Configuration
- **Updated**: All Kafka configurations now use single-node setup (`kafka:9092`)
- **Removed**: Old multi-broker configuration (`localhost:29092,localhost:39092,localhost:49092`)
- **Services**: client, processor, server

### GraphDB Configuration
- **Updated**: All RDF4J configurations now use correct GraphDB URL (`http://graphdb:7200`)
- **Removed**: Old localhost reference (`http://GGKMAC23066.local:7201`)
- **Services**: processor, server

### Service Discovery
- **Updated**: Eureka configuration to use consistent environment variables
- **Fixed**: Service discovery URL to use container name (`service-discovery:8080`)
- **Standardized**: Port configuration across all services

## Usage

The Config Server automatically serves these configurations to the microservices based on:
1. **Service name**: `client`, `processor`, `server`, `servicediscovery`
2. **Profile**: `dev`, `kafka`, `rdf4j`, `https`, etc.
3. **Environment**: Development, test, production

### Example Configuration URLs
- Client dev config: `http://config-server:8080/client/dev`
- Server with Kafka: `http://config-server:8080/server/kafka`
- Processor with RDF4J: `http://config-server:8080/processor/rdf4j`

## Validation

To validate the configuration:
1. Start the Config Server
2. Access configuration endpoints directly
3. Check service startup logs for configuration loading
4. Verify environment variable resolution

### Health Check
```bash
curl http://localhost:8888/actuator/health
```

### Configuration Endpoint
```bash
curl http://localhost:8888/client/dev
``` 