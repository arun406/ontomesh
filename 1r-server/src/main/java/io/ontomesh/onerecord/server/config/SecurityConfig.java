package io.ontomesh.onerecord.server.config;

import io.ontomesh.onerecord.server.services.authorization.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String LOGISTICS_AGENT_URI = "logistics_agent_uri";

    private final AuthorizationService authorizationService;

    public SecurityConfig(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
//                .csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/").permitAll()
                        .requestMatchers(HttpMethod.POST, "/logistics-objects").permitAll()
                        .requestMatchers(HttpMethod.POST, "/notifications").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(
//                        Customizer.withDefaults()
                        jwt -> jwt.jwtAuthenticationConverter(oneRecordJwtAuthenticationConverter())
                ));
        http.addFilterAfter(new OneRecordAuthorizationFilter(this.authorizationService), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> oneRecordJwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(oneRecordJwtGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName(LOGISTICS_AGENT_URI);
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> oneRecordJwtGrantedAuthoritiesConverter() {
        return new MappingJwtGrantedAuthoritiesConverter();
    }
}