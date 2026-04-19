package com.payflow.gateway.proxy;

import com.payflow.gateway.config.RouteDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyService {

    private final HttpForwarder httpForwarder;

    public void proxy(HttpServletRequest request,
                      HttpServletResponse response,
                      RouteDefinition route) throws IOException {

        String target = route.getTarget();
        String cbName = switch (route.getId()) {
            case "accounts", "payments", "auth" -> "payment-service";
            case "ledger"                        -> "ledger-service";
            case "webhooks"                      -> "notification-service";
            default                              -> "payment-service";
        };

        log.debug("Routing {} → {} (cb={})",
                route.getPath(), target, cbName);

        httpForwarder.forwardWithCircuitBreaker(
                request, response, target, cbName
        );
    }
}