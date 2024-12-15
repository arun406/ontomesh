package com.innovasolutions.framework.webclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryRegisteredWebClientRepository implements RegisteredWebClientRepository, Iterable<RegisteredWebClient> {

    private final Map<String, RegisteredWebClient> registrations;

    public InMemoryRegisteredWebClientRepository(RegisteredWebClient... registrations) {
        this(Arrays.asList(registrations));
    }

    /**
     * @param registrations
     */
    public InMemoryRegisteredWebClientRepository(List<RegisteredWebClient> registrations) {
        this(createWebClientRegistrationsMap(registrations));
    }

    /**
     * @param registrations
     * @return
     */
    private static Map<String, RegisteredWebClient> createWebClientRegistrationsMap(List<RegisteredWebClient> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        return toUnmodifiableConcurrentMap(registrations);
    }

    /**
     * @param registrations
     * @return
     */
    private static Map<String, RegisteredWebClient> toUnmodifiableConcurrentMap(List<RegisteredWebClient> registrations) {
        ConcurrentHashMap<String, RegisteredWebClient> result = new ConcurrentHashMap<>();
        for (RegisteredWebClient registration : registrations) {
            Assert.notNull(registration.getClientId(), "client id cannot be null");
            Assert.notNull(registration.getServiceKey(), "service id cannot be null");
            Assert.isTrue(registration.getServiceKey().matches("^[a-zA-Z0-9]+$"),
                    "service id should be alphanumeric. special characters are not allowed.");
//            Assert.isTrue(registration.getClientId().matches("^[a-zA-Z0-9]+$"),
//                    "client id should be alphanumeric only. special characters are not allowed.");
            log.debug("registering the webclient {}", registration.getClientId());

            Assert.state(!result.containsKey(registration.getClientId()),
                    () -> String.format("Duplicate key %s", registration.getClientId()));
            result.put(String.join(".", registration.getClientId(), registration.getServiceKey()), registration);
        }
        return Collections.unmodifiableMap(result);
    }

    public InMemoryRegisteredWebClientRepository(Map<String, RegisteredWebClient> registrations) {
        Assert.notNull(registrations, "registrations cannot be null");
        this.registrations = registrations;
    }


    @Override
    public RegisteredWebClient findByClientAndServiceId(String clientId, String serviceId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        Assert.hasText(serviceId, "serviceId cannot be empty");
        return this.registrations.get(String.join(".", clientId, serviceId));
    }

    @Override
    public Iterator<RegisteredWebClient> iterator() {
        return this.registrations.values().iterator();
    }
}
