# Innova WebClient Configuration Guide

This document provides an overview of the declarative WebClient configuration structure used in Innova Solutions. The YAML configuration file allows defining multiple WebClients with their respective endpoints, methods, headers, request bodies, and expected responses. This enables flexible and centralized management of external service integrations.

## Configuration Structure

### Root Structure

The root of the configuration file includes the main `app` object, containing a list of `webclients`. Each `webclient` represents a distinct client configuration for interacting with a specific external service or API.

### WebClient Configuration

Each WebClient configuration contains the following main attributes:

- **name**: A descriptive name for the WebClient (e.g., `User Client`).
- **clientId**: A unique identifier for the client, used for referencing and identification. **Note**: `clientId` can only contain alphanumeric characters (letters and numbers) and must not include spaces or special characters.
- **baseUrl**: The base URL for the WebClient, which is prepended to each endpoint path defined within the client.
- **description**: A brief description of the client and its purpose.
- **services**: A list of services provided by this WebClient, where each service defines a specific API endpoint with its properties.

### Service Configuration

Each service within a WebClient corresponds to a specific API endpoint and contains the following properties:

- **key**: A unique identifier for the service within the client, often representing the action or resource accessed. **Note**: Similar to `clientId`, `key` can only contain alphanumeric characters and must not include spaces or special characters.
- **endpoint**: Defines the details for making the request to the API endpoint.
  - **method**: The HTTP method to use for the request, such as `GET`, `POST`, `PUT`, or `DELETE`.
  - **path**: The relative path for the endpoint. Path variables can be specified using curly braces (e.g., `/users/{id}`).
  - **headers**: Optional list of HTTP headers to include with the request.
    - **key**: Header name (e.g., `Content-Type` or `Accept`).
    - **value**: Header value (e.g., `application/json`).
  - **requestBody**: (Optional) Specifies the Java class or type for the request payload, if the request requires a body.
  - **responseBody**: Specifies the Java class or type for the expected response payload. This can be a fully qualified class name (e.g., `io.ontomesh.UserResponseDTO`) or a common Java type (e.g., `java.lang.String` or `java.util.List`).

## Example Structure

Below is a generic structure for a sample WebClient configuration:

```yaml
app:
  webclients:
    - name: Client 1
      clientId: client1
      baseUrl: http://example.com
      description: Example client for demonstration
      services:
        - key: sampleService
          endpoint:
            method: GET
            path: /sample/path/{id}
            headers:
              - key: Accept
                value: application/json
            requestBody: null
            responseBody: "com.example.framework.SampleResponseDTO"
```
            
## Usage

### Step 1: Add the Maven Dependency

To use the Innova WebClient , add the following Maven dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>io.ontomesh</groupId>
    <artifactId>innova-webclient</artifactId>
    <version>${framework.version}</version>
</dependency>
```

### Step 2: Autowire RegisteredWebClientRepository
The **RegisteredWebClientRepository** is the central repository for accessing the configured WebClients. Autowire this repository in your class to query and execute services.

```java
    @Autowired
    private RegisteredWebClientRepository repository;
```

### Step 3: Executing Services
**Query the Service**: Use the method _findByClientAndServiceId_(clientId, serviceKey) to retrieve the configured RegisteredWebClient.

**Execute the Service**: Call _execute()_ on the _RegisteredWebClient_, passing any required path variables or request body if needed.

## Example:

```java
import io.ontomesh.webclient.RegisteredWebClientRepository;
import io.ontomesh.webclient.RegisteredWebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebClientService {

    @Autowired
    private RegisteredWebClientRepository repository;

    public void executeGetUserById() {
        try {
            // Query for a client and service by client ID and service key
            RegisteredWebClient registeredWebClient = repository.findByClientAndServiceId("userclient", "getUserById");

            // Validate that the client-service pair exists
            if (registeredWebClient == null) {
              System.out.println("Client or Service not found!");
              return;
            }
            // Execute the service
            Mono<UserResponseDTO> response = registeredWebClient.execute(Map.of("id", "1"));

            // Process the response (e.g., print or log it)
            response.subscribe(user -> System.out.println("User Name: " + user.getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

```

## Notes
**Path Variables:** Path variables in the path should be supplied at runtime.

**Headers:** Custom headers are defined for each service.

**Request and Response Types:** Use fully qualified Java class names for request and response types to ensure proper serialization and deserialization.

**clientId and service.key:** Both clientId and service.key must contain only alphanumeric characters (letters and numbers) without spaces or special characters.