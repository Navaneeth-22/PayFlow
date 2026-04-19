package com.payflow.gateway.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;


@Component
@Slf4j
public class HttpForwarder {

    private final RestClient restClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static final Set<String> SKIP_REQUEST_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding", "connection"
    );

    private static final Set<String> SKIP_RESPONSE_HEADERS = Set.of(
            "transfer-encoding", "connection"
    );

    public HttpForwarder(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", "PayFlow-Gateway/1.0")
                .build();
    }


    public void forwardWithCircuitBreaker(HttpServletRequest request,
                                          HttpServletResponse response,
                                          String targetBase,
                                          String circuitBreakerName)
            throws IOException {

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(
                circuitBreakerName
        );

        log.debug("CB [{}] state={}", circuitBreakerName, cb.getState());

        try {
            cb.executeCheckedSupplier(() -> {
                doForward(request, response, targetBase);
                return null;
            });

        } catch (CallNotPermittedException ex) {
            log.warn("Circuit OPEN [{}] — rejecting request to {}",
                    circuitBreakerName, targetBase);
            writeFallback(response, circuitBreakerName);

        } catch (ResourceAccessException ex) {
            log.error("Network error [{}]: {}", circuitBreakerName, ex.getMessage());
            writeFallback(response, circuitBreakerName);

        } catch (IOException ex) {
            throw ex;

        } catch (Throwable ex) {
            log.error("Unexpected error [{}]: {}", circuitBreakerName, ex.getMessage());
            writeFallback(response, circuitBreakerName);
        }
    }

    private void doForward(HttpServletRequest request,
                           HttpServletResponse response,
                           String targetBase) throws IOException {

        String path  = request.getRequestURI();
        String query = request.getQueryString();
        String url   = targetBase + path + (query != null ? "?" + query : "");
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        log.debug("Forwarding {} → {}", method, url);

        try {
            var spec = restClient.method(method).uri(url)
                    .headers(headers ->
                            Collections.list(request.getHeaderNames()).forEach(name -> {
                                if (!SKIP_REQUEST_HEADERS.contains(name.toLowerCase())) {
                                    headers.add(name, request.getHeader(name));
                                }
                            })
                    );

            ResponseEntity<byte[]> downstream;
            if (hasBody(method)) {
                byte[] body = StreamUtils.copyToByteArray(
                        request.getInputStream());
                downstream = ((RestClient.RequestBodySpec) spec)
                        .body(body)
                        .retrieve()
                        .toEntity(byte[].class);
            } else {
                downstream = spec.retrieve().toEntity(byte[].class);
            }

            writeResponse(response, downstream);

        } catch (RestClientResponseException ex) {
            log.warn("Downstream {} from {}", ex.getStatusCode(), url);
            response.setStatus(ex.getStatusCode().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            byte[] body = ex.getResponseBodyAsByteArray();
            if (body.length > 0) {
                response.getOutputStream().write(body);
            }

        } catch (ResourceAccessException ex) {
            log.error("Cannot reach {}: {}", url, ex.getMessage());
            throw ex;
        }
    }

    private void writeResponse(HttpServletResponse response,
                               ResponseEntity<byte[]> downstream)
            throws IOException {
        response.setStatus(downstream.getStatusCode().value());
        downstream.getHeaders().forEach((name, values) -> {
            if (!SKIP_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                values.forEach(v -> response.addHeader(name, v));
            }
        });
        byte[] body = downstream.getBody();
        if (body != null && body.length > 0) {
            response.getOutputStream().write(body);
        }
    }

    private boolean hasBody(HttpMethod method) {
        return method == HttpMethod.POST
                || method == HttpMethod.PUT
                || method == HttpMethod.PATCH;
    }

    private void writeFallback(HttpServletResponse response,
                               String service) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "30");
        response.getWriter().write(String.format(
                "{\"status\":503,\"error\":\"Service Unavailable\"," +
                        "\"message\":\"%s is temporarily unavailable. " +
                        "Retry after 30 seconds.\"," +
                        "\"service\":\"api-gateway\",\"timestamp\":\"%s\"}",
                service, Instant.now()
        ));
    }
}