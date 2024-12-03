package io.ontomesh.onerecord.server.config;

import io.ontomesh.onerecord.server.services.AuthorizationService;
import io.ontomesh.onerecord.server.util.URIUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Slf4j
public class OneRecordAuthorizationFilter extends OncePerRequestFilter {

    public OneRecordAuthorizationFilter(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    private final AuthorizationService authorizationService;

    /**
     * Perform Authorization.
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.trace("checking for the authorization.");
        // Get the Authentication object from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If user is not authenticated, deny access
        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Forbidden: You must be authenticated to access this resource.");
            return;
        }

        // Retrieve the principal (e.g., username or user ID) from the authentication
        Object principal = authentication.getPrincipal();

        Assert.notNull(principal, "Agent cannot be null.");

        IRI agent = null;
        // Depending on the type of principal, extract the user ID (it could be a string or an instance of a custom UserDetails class)
        if (principal instanceof Jwt jwt) {
            String logisticsAgentUri = jwt.getClaim("logistics_agent_uri");
            agent = SimpleValueFactory.getInstance().createIRI(logisticsAgentUri); // Assuming the principal is the user ID or username
        }

        // Extract the requested URL
        URI uri = ServletUriComponentsBuilder.fromRequestUri(request)
                .build()
                .toUri();
        IRI resource = URIUtil.toIRI(uri);
        String method = request.getMethod();

        IRI mode = switch (method) {
            case "GET" -> SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/auth/acl#Read");
            case "POST", "PATCH", "DELETE", "PUT" ->
                    SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/auth/acl#Write");
            default -> throw new IllegalStateException(String.format("Unexpected method: %s", method));
        };
        boolean hasAccess = false;
        if (agent != null && agent.stringValue().equals("http://server.ontomesh.io/logistics-objects/ontomesh")) {
            hasAccess = true;
        } else {
            // Use RDFAuthorizationService to check if the user has access to the requested URL
            hasAccess = authorizationService.hasAccessToResource(agent, resource, mode);
        }
        if (!hasAccess) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Forbidden: You do not have access to this resource.");
            return; // Deny access if not authorized
        }

        // Proceed with the request chain if access is granted
        filterChain.doFilter(request, response);
    }
}
