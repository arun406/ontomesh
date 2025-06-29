package io.ontomesh.onerecord.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class InternalEndpointBypassFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        log.debug("InternalEndpointBypassFilter checking path: {}", path);
        // Only apply this filter to internal endpoints
        return !path.matches("^/logistics-objects/internal/.*$");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        log.debug("Bypassing OAuth2 authentication for internal endpoint: {}", request.getRequestURI());
        
        // Create an anonymous authentication for internal endpoints
        AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                "internal-endpoint",
                "internal-user",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL"))
        );
        
        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(anonymousAuth);
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
} 