package com.payflow.payment.idempotency;

import com.payflow.payment.exception.MissingIdempotencyKeyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

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
            throw new MissingIdempotencyKeyException();
        }

        Optional<IdempotencyRecord> cached = idempotencyService.findInCache(key);
        if (cached.isPresent()) {
            log.debug("Idempotency HIT (Redis) for key: {}", key);
            writeStoredResponse(response, cached.get());
            return;
        }

        Optional<IdempotencyRecord> dbRecord = idempotencyService.findInDb(key);
        if (dbRecord.isPresent()) {
            log.debug("Idempotency HIT (DB) for key: {}", key);
            idempotencyService.repopulateCache(key, dbRecord.get());
            writeStoredResponse(response, dbRecord.get());
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request,0);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        chain.doFilter(wrappedRequest, wrappedResponse);

        byte[] requestBody = wrappedRequest.getContentAsByteArray();
        String requestHash = idempotencyService.computeHash(requestBody);

        int status = wrappedResponse.getStatus();
        byte[] responseBody = wrappedResponse.getContentAsByteArray();

        if (status >= 200 && status < 300 && responseBody.length > 0) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                idempotencyService.store(key, requestHash, status, bodyMap);
            } catch (Exception e) {
                log.error("Failed to store idempotency record for key: {}", key, e);
            }
        }
        wrappedResponse.copyBodyToResponse();
    }

    private void writeStoredResponse(HttpServletResponse response,
                                     IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(record.getResponseBody())
        );
    }
}