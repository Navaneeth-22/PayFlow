package com.payflow.gateway.security;

import com.payflow.gateway.config.RouteDefinition;
import com.payflow.gateway.config.RouteRegistry;
import io.jsonwebtoken.Claims;
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
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RouteRegistry routeRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        Optional<RouteDefinition> route = routeRegistry.match(path);

        if (route.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        if (!route.get().isAuthRequired()) {
            chain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED,
                    "Authorization header required. Format: Bearer <token>");
            return;
        }

        Claims claims = jwtUtil.validateAndExtract(authHeader.substring(7));

        if (claims == null) {
            writeError(response, HttpStatus.UNAUTHORIZED,
                    "Invalid or expired JWT token");
            return;
        }

        String userId = claims.getSubject();
        log.debug("Authenticated userId={} path={}", userId, path);

        MutableHttpServletRequest mutable =
                new MutableHttpServletRequest(request);
        mutable.putHeader("X-User-Id", userId);

        chain.doFilter(mutable, response);
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"," +
                        "\"service\":\"api-gateway\",\"timestamp\":\"%s\"}",
                status.value(), status.getReasonPhrase(),
                message, Instant.now()
        ));
    }
}