package com.payflow.payment;

import com.payflow.payment.api.dto.CreatePaymentRequest;
import com.payflow.payment.domain.model.Account;
import com.payflow.payment.domain.model.AccountStatus;
import com.payflow.payment.domain.repository.AccountRepository;
import com.payflow.payment.domain.repository.PaymentRepository;
import com.payflow.payment.domain.repository.SagaStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Saga Happy Path Integration Tests")
class SagaHappyPathIT extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    private UUID accountAId;
    private UUID accountBId;

    @BeforeEach
    void setup() {
        accountAId = accountRepository.save(Account.builder()
                .userId(UUID.randomUUID())
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .build()).getId();

        accountBId = accountRepository.save(Account.builder()
                .userId(UUID.randomUUID())
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .build()).getId();
    }

    @Test
    @DisplayName("Payment initiated → 202 accepted → saga created in DB")
    void paymentInitiated_returns202_sagaCreated() throws Exception {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setFromAccountId(accountAId);
        request.setToAccountId(accountBId);
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("INR");

        String responseBody = mockMvc.perform(
                        post("/api/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(
                objectMapper.readTree(responseBody)
                        .get("paymentId").asText()
        );

        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(sagaStateRepository.findById(paymentId)).isPresent();
            assertThat(sagaStateRepository.findById(paymentId)
                    .get().getLastEvent())
                    .isEqualTo("PAYMENT_INITIATED");
        });

        assertThat(paymentRepository.findById(paymentId)).isPresent();
        assertThat(paymentRepository.findById(paymentId)
                .get().getAmount())
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Same idempotency key + same body → same paymentId, 1 payment in DB")
    void sameKeyAndBody_returnsIdenticalResponse_onePaymentInDb()
            throws Exception {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setFromAccountId(accountAId);
        request.setToAccountId(accountBId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");

        String idempotencyKey = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(request);

        String first = mockMvc.perform(
                        post("/api/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Idempotency-Key", idempotencyKey)
                                .content(body)
                )
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(
                        post("/api/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Idempotency-Key", idempotencyKey)
                                .content(body)
                )
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String firstId = objectMapper.readTree(first)
                .get("paymentId").asText();
        String secondId = objectMapper.readTree(second)
                .get("paymentId").asText();
        assertThat(firstId).isEqualTo(secondId);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            long count = paymentRepository.findAll().stream()
                    .filter(p -> p.getIdempotencyKey().equals(idempotencyKey))
                    .count();
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Same key + different body → 422")
    void sameKeyDifferentBody_returns422() throws Exception {

        String key = UUID.randomUUID().toString();

        CreatePaymentRequest first = new CreatePaymentRequest();
        first.setFromAccountId(accountAId);
        first.setToAccountId(accountBId);
        first.setAmount(new BigDecimal("100.00"));
        first.setCurrency("INR");

        CreatePaymentRequest second = new CreatePaymentRequest();
        second.setFromAccountId(accountAId);
        second.setToAccountId(accountBId);
        second.setAmount(new BigDecimal("999.00")); // different
        second.setCurrency("INR");

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", key)
                .content(objectMapper.writeValueAsString(first)));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", key)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Missing idempotency key → 400")
    void missingKey_returns400() throws Exception {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setFromAccountId(accountAId);
        request.setToAccountId(accountBId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("X-Idempotency-Key header is required"));
    }

    @Test
    @DisplayName("Unknown fromAccountId → 404")
    void unknownAccount_returns404() throws Exception {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setFromAccountId(UUID.randomUUID());
        request.setToAccountId(accountBId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Self transfer → 400")
    void selfTransfer_returns400() throws Exception {

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setFromAccountId(accountAId);
        request.setToAccountId(accountAId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}