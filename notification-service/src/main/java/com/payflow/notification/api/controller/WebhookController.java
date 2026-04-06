package com.payflow.notification.api.controller;

import com.payflow.notification.domain.model.Webhook;
import com.payflow.notification.domain.model.WebhookDelivery;
import com.payflow.notification.domain.repository.WebhookDeliveryRepository;
import com.payflow.notification.domain.repository.WebhookRepository;
import com.payflow.notification.domain.service.WebhookSigningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSigningService signingService;


    @PostMapping
    public ResponseEntity<Map<String, Object>> register( @Valid @RequestBody RegisterRequest request) {

        String secret = signingService.generateSecret();

        Webhook webhook = Webhook.builder()
                .merchantId(request.getMerchantId())
                .url(request.getUrl())
                .secret(secret)
                .events(request.getEvents())
                .build();

        webhookRepository.save(webhook);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "webhookId", webhook.getId().toString(),
                "url",       webhook.getUrl(),
                "events",    webhook.getEvents(),
                "secret",    secret,
                "warning",   "Save this secret now — it will never be shown again"
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam UUID merchantId) {

        return ResponseEntity.ok(
                webhookRepository.findByMerchantIdAndActiveTrue(merchantId)
                        .stream()
                        .map(w -> Map.<String, Object>of(
                                "webhookId", w.getId().toString(),
                                "url",       w.getUrl(),
                                "events",    w.getEvents(),
                                "active",    w.getActive(),
                                "createdAt", w.getCreatedAt().toString()
                        ))
                        .toList()
        );
    }

    @DeleteMapping("/{webhookId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID webhookId) {
        webhookRepository.findById(webhookId).ifPresent(w -> {
            w.setActive(false);
            webhookRepository.save(w);
        });
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{webhookId}/deliveries")
    public ResponseEntity<Page<WebhookDelivery>> deliveries(
            @PathVariable UUID webhookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                deliveryRepository.findByWebhookIdOrderByCreatedAtDesc(
                        webhookId, PageRequest.of(page, size))
        );
    }

    @Data
    public static class RegisterRequest {
        @NotNull
        private UUID merchantId;

        @NotBlank
        private String url;

        @NotEmpty
        private List<String> events;
    }
}