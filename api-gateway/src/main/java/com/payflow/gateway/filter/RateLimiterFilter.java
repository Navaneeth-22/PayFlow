package com.payflow.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final double REFILL_RATE    = 10.0;
    private static final double BURST_CAPACITY = 20.0;

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT =
            new DefaultRedisScript<>("""
            local tokens_key      = KEYS[1]
            local last_refill_key = KEYS[2]
            local refill_rate     = tonumber(ARGV[1])
            local capacity        = tonumber(ARGV[2])
            local now             = tonumber(ARGV[3])
            local ttl             = tonumber(ARGV[4])

            local tokens = tonumber(redis.call('GET', tokens_key))
            if tokens == nil then tokens = capacity end

            local last = tonumber(redis.call('GET', last_refill_key))
            if last == nil then last = now end

            -- Calculate tokens to add since last request
            local elapsed = (now - last) / 1000.0
            tokens = math.min(capacity, tokens + elapsed * refill_rate)

            redis.call('SET', last_refill_key, now, 'EX', ttl)

            if tokens < 1 then
                redis.call('SET', tokens_key, tokens, 'EX', ttl)
                return 0
            end

            redis.call('SET', tokens_key, tokens - 1, 'EX', ttl)
            return 1
            """,
                    Long.class
            );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }

        String userId    = request.getHeader("X-User-Id");
        String bucketKey = userId != null
                ? "rl:user:" + userId
                : "rl:ip:"   + getClientIp(request);

        boolean allowed = tryConsume(bucketKey);

        if (!allowed) {
            log.warn("Rate limit exceeded: key={} path={}", bucketKey, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "1");
            response.setHeader("X-RateLimit-Limit",
                    String.valueOf((int) BURST_CAPACITY));
            response.getWriter().write(String.format(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                            "\"message\":\"Rate limit exceeded. Refills at %d tokens/s.\"," +
                            "\"service\":\"api-gateway\",\"timestamp\":\"%s\"}",
                    (int) REFILL_RATE, Instant.now()
            ));
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean tryConsume(String bucketKey) {
        Long result = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(bucketKey + ":tokens", bucketKey + ":lastrefill"),
                String.valueOf(REFILL_RATE),
                String.valueOf(BURST_CAPACITY),
                String.valueOf(System.currentTimeMillis()),
                "60"
        );
        return result != null && result == 1L;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}