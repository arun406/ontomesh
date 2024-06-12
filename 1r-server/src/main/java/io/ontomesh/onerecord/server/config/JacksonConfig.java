package io.ontomesh.onerecord.server.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.ontomesh.onerecord.server.api.ActionRequestSerializer;
import io.ontomesh.onerecord.server.domain.ActionRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public com.fasterxml.jackson.databind.Module actionRequestSerializer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ActionRequest.class, new ActionRequestSerializer());
        return module;
    }
}
