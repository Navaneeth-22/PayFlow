package com.payflow.gateway.config;

import lombok.Data;

@Data
public class RouteDefinition {
    private String id;
    private String path;
    private String target;
    private boolean authRequired;
}