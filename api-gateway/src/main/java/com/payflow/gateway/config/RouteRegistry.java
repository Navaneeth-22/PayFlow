package com.payflow.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;


@Slf4j
public class RouteRegistry {

    private final List<RouteDefinition> routes;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RouteRegistry(List<RouteDefinition> routes) {
        this.routes = routes;
    }

    public Optional<RouteDefinition> match(String requestUri) {
        return routes.stream()
                .filter(route -> pathMatcher.match(route.getPath(), requestUri))
                .findFirst();
    }

    public List<RouteDefinition> getAllRoutes() {
        return routes;
    }
}