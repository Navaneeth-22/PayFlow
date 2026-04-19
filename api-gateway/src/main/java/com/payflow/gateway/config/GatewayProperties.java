package com.payflow.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway")
@Data
public class GatewayProperties {
    private List<RouteDefinition> routes = new ArrayList<>();
}