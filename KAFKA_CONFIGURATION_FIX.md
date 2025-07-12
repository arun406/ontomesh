# Kafka Configuration Fix for OntoMesh Event Processing

## Issue Summary

The 1r-processor was not receiving events from the 1r-server due to configuration mismatches in Kafka topic names, client IDs, and group IDs.

## Problems Identified

### 1. Topic Name Mismatch
- **Server published to:** `logistics-server` (from `onerecord.logistics.events.destination`)
- **Processor listened to:** `logistics-processor` (from `KAFKA_TOPIC_PROCESSOR`)
- **Expected topic:** `logistics-events` (as specified in `.env` file)

### 2. Client ID and Group ID Conflicts
- All services were using the same `KAFKA_CLIENT_ID` and `KAFKA_GROUP_ID`
- This could cause conflicts in Kafka consumer group management

### 3. Environment Variable Inconsistencies
- Environment variables didn't match the actual configuration values
- Different services had conflicting topic configurations

## Fixes Applied

### 1. Updated Server Configuration
**Files Modified:**
- `config-repo/server/application-airline-onerecord.yml`
- `config-repo/server/application-forwarder-onerecord.yml`

**Changes:**
```yaml
onerecord:
  logistics:
    events:
      destination: ${KAFKA_TOPIC_PROCESSOR:logistics-events}  # Changed from logistics-server
```

### 2. Updated Processor Configuration
**File Modified:**
- `config-repo/processor/application-kafka.yml`

**Changes:**
```yaml
bindings:
  input:
    destination: ${KAFKA_TOPIC_PROCESSOR:logistics-events}  # Changed from logistics-processor
    group: logistics-processor-group-01  # Changed from logistics-group-01
```

### 3. Updated Client Configuration
**File Modified:**
- `config-repo/client/application-kafka.yml`

**Changes:**
```yaml
bindings:
  input:
    destination: ${KAFKA_TOPIC_CLIENT:logistics-client}
    group: notification-client-group-01  # Changed from notification-group-01
```

### 4. Updated Environment Variables
**File Modified:**
- `deployment/application/env.example`

**Changes:**
```bash
# Client Configuration
KAFKA_TOPIC_CLIENT=logistics-events-notification  # Changed from logistics-events

# Kafka Configuration
KAFKA_CLIENT_ID=ontomesh-server  # Changed from ontomesh-client
KAFKA_GROUP_ID=ontomesh-server-group  # Changed from ontomesh-group
```

### 5. Updated Docker Compose Configuration
**File Modified:**
- `deployment/application/docker-compose.yml`

**Changes:**
- Added unique client IDs and group IDs for each service:
  - **1r-client:** `ontomesh-client` / `ontomesh-client-group`
  - **1r-processor:** `ontomesh-processor` / `ontomesh-processor-group`
  - **1r-server:** `ontomesh-server` / `ontomesh-server-group`

## Event Flow After Fix

1. **1r-server** publishes events to topic: `logistics-events`
2. **1r-processor** consumes events from topic: `logistics-events`
3. **1r-processor** publishes notifications to topic: `logistics-events-notification`
4. **1r-client** consumes notifications from topic: `logistics-events-notification`

## Verification Steps

### 1. Check Topic Names
```bash
# Verify the topics exist in Kafka
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

Expected topics:
- `logistics-events`
- `logistics-events-notification`

### 2. Check Consumer Groups
```bash
# Verify consumer groups are properly configured
docker exec -it kafka kafka-consumer-groups --list --bootstrap-server localhost:9092
```

Expected groups:
- `logistics-processor-group-01`
- `notification-client-group-01`

### 3. Monitor Event Flow
```bash
# Monitor events being published
docker exec -it kafka kafka-console-consumer --topic logistics-events --bootstrap-server localhost:9092

# Monitor notifications being published
docker exec -it kafka kafka-console-consumer --topic logistics-events-notification --bootstrap-server localhost:9092
```

### 4. Check Application Logs
```bash
# Check server logs for successful event publishing
docker logs 1r-server | grep "EventProducer"

# Check processor logs for successful event consumption
docker logs 1r-processor | grep "EVENT PROCESSOR"
```

## Configuration Summary

| Service | Client ID | Group ID | Input Topic | Output Topic |
|---------|-----------|----------|-------------|--------------|
| 1r-server | ontomesh-server | ontomesh-server-group | - | logistics-events |
| 1r-processor | ontomesh-processor | logistics-processor-group-01 | logistics-events | logistics-events-notification |
| 1r-client | ontomesh-client | notification-client-group-01 | logistics-events-notification | - |

## Environment Variables

Key environment variables that control the configuration:

```bash
# Topic Configuration
KAFKA_TOPIC_PROCESSOR=logistics-events
KAFKA_TOPIC_CLIENT=logistics-events-notification
LOGISTICS_EVENTS_NOTIFICATION_TOPIC=logistics-events-notification

# Kafka Connection
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Service-specific Client/Group IDs (set in docker-compose.yml)
KAFKA_CLIENT_ID=ontomesh-{service}
KAFKA_GROUP_ID=ontomesh-{service}-group
```

## Troubleshooting

If events are still not being processed:

1. **Check Kafka connectivity:**
   ```bash
   docker exec -it 1r-processor curl -f http://kafka:9092
   ```

2. **Verify topic exists:**
   ```bash
   docker exec -it kafka kafka-topics --describe --topic logistics-events --bootstrap-server localhost:9092
   ```

3. **Check consumer group status:**
   ```bash
   docker exec -it kafka kafka-consumer-groups --describe --group logistics-processor-group-01 --bootstrap-server localhost:9092
   ```

4. **Monitor application logs:**
   ```bash
   docker logs -f 1r-processor
   docker logs -f 1r-server
   ```

## Next Steps

1. Restart all services to apply the configuration changes
2. Test event publishing and consumption
3. Monitor logs for any remaining issues
4. Verify that notifications are being sent to clients 