package com.payflow.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayRoutesConfig {

    private final GatewayProperties gatewayProperties;

    @Bean
    public RouteRegistry routeRegistry() {
        RouteRegistry registry = new RouteRegistry(
                gatewayProperties.getRoutes()
        );
        log.info("Gateway loaded {} routes:", gatewayProperties.getRoutes().size());
        gatewayProperties.getRoutes().forEach(r ->
                log.info("  {} → {} (auth={})", r.getPath(), r.getTarget(), r.isAuthRequired())
        );
        return registry;
    }
}