package com.innovasolutions.framework.webclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innovasolutions.framework.UserResponseDTO;
import com.innovasolutions.framework.webclient.config.WebClientProperties;
import com.innovasolutions.framework.webclient.config.WebClientServiceConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = WebClientProperties.class)
@ContextConfiguration(classes = {WebClientServiceConfig.class}, initializers = ConfigDataApplicationContextInitializer.class)
public class RegisteredWebClientTest {

    @Autowired
    private RegisteredWebClientRepository repository;

    @Autowired
    private WebClientProperties webClientProperties;

    @BeforeEach
    public void setup() {
    }

    @Test
    public void testConfigurations() {
        assertNotNull(repository.findByClientAndServiceId("client1", "getUserById"));
        assertNotNull(webClientProperties);
    }

    @Test
    public void testGetAllUsers() throws ClassNotFoundException {
        RegisteredWebClient byClientAndServiceId = repository.findByClientAndServiceId("userclient", "getAllUsers");
        assertNotNull(byClientAndServiceId);
//        List<LinkedHashMap<String, String>> response = (List<LinkedHashMap<String, String>>) byClientAndServiceId.execute().block();
//        assertNotNull(response);
        // Prepare a mock response with JSON content
        String mockResponseJson = "[{ \"id\": 1, \"name\": \"John Doe\" }, { \"id\": 2, \"name\": \"Jane Doe\" }]";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponseJson)
                .addHeader("Content-Type", "application/json"));

        RegisteredWebClient registeredWebClient = Mockito.spy(byClientAndServiceId);
        Mockito.when(registeredWebClient.getWebClient()).thenReturn(webClient);

        Mono<List<LinkedHashMap<String, String>>> response = registeredWebClient.execute();
        assertNotNull(response);


//        assertEquals(userResponseDTOS.size(), 2);
//        assertEquals(userResponseDTOS.getFirst().name(), "arun kumar");

        // Verify the response
        StepVerifier
                .create(response)
                .expectNextMatches(linkedHashMaps -> {
                    List<UserResponseDTO> userResponseDTOS = new ObjectMapper().convertValue(linkedHashMaps, new TypeReference<>() {
                    });
                    assertEquals(userResponseDTOS.size(), 2);
                    assertEquals(userResponseDTOS.getFirst().name(), "John Doe");
                    return true;
                })
                .verifyComplete();

    }

    @Test
    public void testSaveUser() throws ClassNotFoundException {
        RegisteredWebClient byClientAndServiceId = repository.findByClientAndServiceId("userclient", "saveUser");
        assertNotNull(byClientAndServiceId);

        // Prepare a mock response with JSON content
        String mockResponseJson = "user saved successfully";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        RegisteredWebClient spyClientAndServiceId = Mockito.spy(byClientAndServiceId);
        Mockito.when(spyClientAndServiceId.getWebClient()).thenReturn(webClient);

        // Execute
        Mono<String> response = byClientAndServiceId.execute(new UserResponseDTO(1, "John Doe"));
        assertNotNull(response);
        // Verify the response
        StepVerifier
                .create(response)
                .expectNextMatches(s -> s.equals("user saved successfully"))
                .verifyComplete();
    }

    @Test
    public void testUpdateUser() throws ClassNotFoundException {
        RegisteredWebClient byClientAndServiceId = repository.findByClientAndServiceId("userclient", "updateUser");
        assertNotNull(byClientAndServiceId);
        // Prepare a mock response with JSON content
        String mockResponseJson = "{ \"id\": 1, \"name\": \"John Doe\" }";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        RegisteredWebClient spyClientAndServiceId = Mockito.spy(byClientAndServiceId);
        Mockito.when(spyClientAndServiceId.getWebClient()).thenReturn(webClient);

        // Execute
        Mono<Void> response = spyClientAndServiceId
                .execute(new UserResponseDTO(1, "John Doe"), Map.of("id", "1"));

        // Verify the response
        StepVerifier
                .create(response)
                .verifyComplete();
    }

    @Test
    public void testDeleteUser() throws ClassNotFoundException {
        RegisteredWebClient byClientAndServiceId = repository.findByClientAndServiceId("userclient", "deleteUser");
        assertNotNull(byClientAndServiceId);

        //mock
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));
        RegisteredWebClient spyClientAndServiceId = Mockito.spy(byClientAndServiceId);
        Mockito.when(spyClientAndServiceId.getWebClient()).thenReturn(webClient);
        // Execute
        Mono<Void> deleteResponse = byClientAndServiceId.execute(Map.of("id", "102"));

        // Verify that the response completes without emitting any item (i.e., it is empty)
        StepVerifier.create(deleteResponse)
                .verifyComplete(); // Confirms that the response completes without error
    }

    private MockWebServer mockWebServer;
    private WebClient webClient;

    @Test
    public void testGetUserById() throws ClassNotFoundException {
        // Prepare a mock response with JSON content
        String mockResponseJson = "{ \"id\": 1, \"name\": \"John Doe\" }";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponseJson)
                .addHeader("Content-Type", "application/json"));
        RegisteredWebClient byClientAndServiceId = repository
                .findByClientAndServiceId("userclient", "getUserById");
        RegisteredWebClient registeredWebClient = Mockito.spy(byClientAndServiceId);
        Mockito.when(registeredWebClient.getWebClient()).thenReturn(webClient);
        Map<String, String> pathVariables = Map.of("id", "1");
        Mono<UserResponseDTO> response = registeredWebClient.execute(pathVariables);
        // Verify the response
        StepVerifier
                .create(response)
                .expectNextMatches(user -> user.id() == 1 && "John Doe".equals(user.name()))
                .verifyComplete();

    }

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Set the WebClient base URL to the MockWebServer's URL
        webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
