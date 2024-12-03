package io.ontomesh.onerecord.server.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MappingJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLES = "roles";
    public static final String DEFAULT_ROLE_PREFIX = "SCOPE";
    public static final String EXTERNAL_ONE_RECORD_CLIENT = "external-one-record-client";

    @SuppressWarnings("unchecked")
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Map<String, Object> resourceAccess = (Map<String, Object>) jwt.getClaimAsMap(RESOURCE_ACCESS)
                .get(EXTERNAL_ONE_RECORD_CLIENT);

        if (resourceAccess != null) {
            List<Object> roles = (List<Object>) resourceAccess.get(ROLES);
            return roles.stream().map(String::valueOf)
                    .map(role -> String.join("_", DEFAULT_ROLE_PREFIX, role))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toCollection(HashSet::new));

        }
        return List.of();
    }
}
