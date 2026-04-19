package com.payflow.gateway.filter;

import com.payflow.gateway.config.RouteDefinition;
import com.payflow.gateway.config.RouteRegistry;
import com.payflow.gateway.proxy.ProxyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class RoutingFilter extends OncePerRequestFilter {

    private final RouteRegistry routeRegistry;
    private final ProxyService  proxyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/actuator/") || path.startsWith("/admin/")) {
            chain.doFilter(request, response);
            return;
        }

        Optional<RouteDefinition> route = routeRegistry.match(path);

        if (route.isEmpty()) {
            log.warn("No route found for path: {}", path);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"status\":404,\"error\":\"Not Found\"," +
                            "\"message\":\"No route configured for path: %s\"," +
                            "\"service\":\"api-gateway\",\"timestamp\":\"%s\"}",
                    path, Instant.now()
            ));
            return;
        }

        log.debug("Routing {} → {}", path, route.get().getTarget());
        proxyService.proxy(request, response, route.get());
    }
}