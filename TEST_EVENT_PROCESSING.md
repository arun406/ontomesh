# Event Processing Test Guide

## Current Status ✅
- 1r-processor is successfully connected to Kafka
- Consumer subscribed to `logistics-events` topic
- All configuration issues have been resolved

## Test Event Processing

### Step 1: Monitor Processor Logs
```bash
docker logs 1r-processor -f
```

### Step 2: Trigger an Event
To test event processing, you can:

1. **Use the server API** to create a logistics object
2. **Check if events are published** to the `logistics-events` topic
3. **Verify the processor receives** and processes the events

### Step 3: Expected Logs
When an event is received, you should see:
```
=== EVENT PROCESSOR: INCOMING EVENT RECEIVED ===
Topic: logistics-events
Event ID: [event-id]
Content Type: application/ld+json
Payload Length: [number] characters
Message Headers: [headers]
Successfully converted payload to RDF model for event: [event-id] from topic: logistics-events
Event model contains [number] statements
Created [number] notification messages for event: [event-id] from topic: logistics-events
=== EVENT PROCESSOR: EVENT PROCESSING COMPLETED for topic: logistics-events ===
```

### Step 4: Check Kafka Topic
```bash
# Check if messages are in the topic
docker exec -it kafka kafka-console-consumer --topic logistics-events --bootstrap-server localhost:9092 --from-beginning --max-messages 5
```

## Issues Resolved ✅

1. **Consumer Disconnection Issue:** Fixed by removing `@EnableAutoConfiguration` and improving Kafka configuration
2. **Topic Name Mismatch:** Both server and processor now use `logistics-events`
3. **Configuration Conflicts:** Unique client/group IDs for each service
4. **Connection Stability:** Added proper timeout and retry configurations

## Next Steps
1. Trigger a test event through the server API
2. Monitor the processor logs for event consumption
3. Verify that notifications are sent to the output topic 