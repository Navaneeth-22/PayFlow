package com.payflow.payment.idempotency;

import com.payflow.payment.exception.IdempotencyKeyMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final String REDIS_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyRepository idempotencyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<IdempotencyRecord> findInCache(String key) {
        Object cached = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
        if (cached == null) return Optional.empty();
        IdempotencyRecord record = objectMapper.convertValue(cached, IdempotencyRecord.class);
        return Optional.of(record);
    }

    public Optional<IdempotencyRecord> findInDb(String key) {
        return idempotencyRepository.findById(key)
                .filter(r -> r.getExpiresAt().isAfter(Instant.now())); // ignore expired records
    }

    public void repopulateCache(String key, IdempotencyRecord record) {
        long secondsLeft = Duration.between(Instant.now(), record.getExpiresAt()).getSeconds();
        if (secondsLeft > 0) {
            redisTemplate.opsForValue()
                    .set(REDIS_PREFIX + key, record, secondsLeft, TimeUnit.SECONDS);
        }
    }

    @Transactional
    public void store(String key, String requestHash,
                      int responseStatus, Map<String, Object> responseBody) {

        Instant expiresAt = Instant.now().plus(TTL);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(key)
                .requestHash(requestHash)
                .status("DONE")
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .expiresAt(expiresAt)
                .build();

        idempotencyRepository.save(record);

        redisTemplate.opsForValue()
                .set(REDIS_PREFIX + key, record, TTL.toSeconds(), TimeUnit.SECONDS);

        log.debug("Stored idempotency record for key: {}", key);
    }


    public void validateRequestHash(IdempotencyRecord existing, String incomingHash) {
        if (!existing.getRequestHash().equals(incomingHash)) {
            throw new IdempotencyKeyMismatchException();
        }
    }


    public String computeHash(byte[] bodyBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bodyBytes);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }
}