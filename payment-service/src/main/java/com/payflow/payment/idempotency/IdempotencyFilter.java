package com.payflow.payment.idempotency;

import com.payflow.payment.exception.IdempotencyKeyMismatchException;
import com.payflow.payment.exception.MissingIdempotencyKeyException;
import com.payflow.payment.metrics.PaymentMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final PaymentMetrics paymentMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        boolean isPaymentPost = "POST".equals(request.getMethod())
                && request.getRequestURI().matches("/api/v1/payments/?");

        if (!isPaymentPost) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader("X-Idempotency-Key");
        if (key == null || key.isBlank()) {
            writeError(response, HttpStatus.BAD_REQUEST,
                    "X-Idempotency-Key header is required");
            return;
        }
        CachedBodyRequestWrapper wrappedRequest =
                new CachedBodyRequestWrapper(request);

        byte[] requestBodyBytes = wrappedRequest.getCachedBody();
        String incomingHash = idempotencyService.computeHash(requestBodyBytes);

        Optional<IdempotencyRecord> cached = idempotencyService.findInCache(key);
        if (cached.isPresent()) {
            log.debug("Idempotency HIT (Redis) for key: {}", key);
            if (!cached.get().getRequestHash().equals(incomingHash)) {
                writeError(response, HttpStatus.UNPROCESSABLE_ENTITY,
                        "Request body does not match original request " +
                                "for this idempotency key");
                paymentMetrics.idempotencyMismatch();
                return;
            }
            writeStoredResponse(response, cached.get());
            paymentMetrics.idempotencyHit();
            return;
        }

        Optional<IdempotencyRecord> dbRecord = idempotencyService.findInDb(key);
        if (dbRecord.isPresent()) {
            log.debug("Idempotency HIT (DB) for key: {}", key);
            if (!dbRecord.get().getRequestHash().equals(incomingHash)) {
                writeError(response, HttpStatus.UNPROCESSABLE_ENTITY,
                        "Request body does not match original request " +
                                "for this idempotency key");
                paymentMetrics.idempotencyMismatch();
                return;
            }
            idempotencyService.repopulateCache(key, dbRecord.get());
            writeStoredResponse(response, dbRecord.get());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse =
                new ContentCachingResponseWrapper(response);

        chain.doFilter(wrappedRequest, wrappedResponse);

        int status = wrappedResponse.getStatus();
        byte[] responseBodyBytes = wrappedResponse.getContentAsByteArray();

        if (status >= 200 && status < 300 && responseBodyBytes.length > 0) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap =
                        objectMapper.readValue(responseBodyBytes, Map.class);
                idempotencyService.store(key, incomingHash, status, bodyMap);
            } catch (Exception e) {
                log.error("Failed to store idempotency record for key: {}", key, e);
            }
        }

        wrappedResponse.copyBodyToResponse();
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of(
                        "status",    status.value(),
                        "error",     status.getReasonPhrase(),
                        "message",   message,
                        "service",   "payment-service",
                        "timestamp", Instant.now().toString()
                ))
        );
    }

    private void writeStoredResponse(HttpServletResponse response,
                                     IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(record.getResponseBody())
        );
    }
}