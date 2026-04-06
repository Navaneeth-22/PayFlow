package com.payflow.notification.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Slf4j
public class WebhookSigningService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );
            mac.init(keySpec);
            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute HMAC signature", e);
            throw new RuntimeException("Signature computation failed", e);
        }
    }
}