package com.innovasolutions.framework.webclient;

public interface RegisteredWebClientRepository {

    RegisteredWebClient findByClientAndServiceId(String clientId, String serviceId);
}
