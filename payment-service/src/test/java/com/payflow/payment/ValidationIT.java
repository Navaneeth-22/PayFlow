package com.payflow.payment;

import com.payflow.payment.api.dto.CreatePaymentRequest;
import com.payflow.payment.domain.model.Account;
import com.payflow.payment.domain.model.AccountStatus;
import com.payflow.payment.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Payment Validation Tests")
class ValidationIT extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    private UUID accountAId;
    private UUID accountBId;

    @BeforeEach
    void setup() {
        accountAId = accountRepository.save(Account.builder()
                .userId(UUID.randomUUID()).currency("INR")
                .status(AccountStatus.ACTIVE).build()).getId();

        accountBId = accountRepository.save(Account.builder()
                .userId(UUID.randomUUID()).currency("INR")
                .status(AccountStatus.ACTIVE).build()).getId();
    }

    private void assertPaymentStatus(UUID from, UUID to,
                                     BigDecimal amount,
                                     String key,
                                     int expectedStatus) throws Exception {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setFromAccountId(from);
        req.setToAccountId(to);
        req.setAmount(amount);
        req.setCurrency("INR");

        var builder = post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req));
        if (key != null) builder.header("X-Idempotency-Key", key);

        mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus));
    }

    @Test @DisplayName("Missing idempotency key → 400")
    void missingKey() throws Exception {
        assertPaymentStatus(accountAId, accountBId,
                BigDecimal.TEN, null, 400);
    }

    @Test @DisplayName("Negative amount → 400")
    void negativeAmount() throws Exception {
        assertPaymentStatus(accountAId, accountBId,
                new BigDecimal("-100"),
                UUID.randomUUID().toString(), 400);
    }

    @Test @DisplayName("Zero amount → 400")
    void zeroAmount() throws Exception {
        assertPaymentStatus(accountAId, accountBId,
                BigDecimal.ZERO,
                UUID.randomUUID().toString(), 400);
    }

    @Test @DisplayName("Self transfer → 400")
    void selfTransfer() throws Exception {
        assertPaymentStatus(accountAId, accountAId,
                BigDecimal.TEN,
                UUID.randomUUID().toString(), 400);
    }

    @Test @DisplayName("Unknown fromAccount → 404")
    void unknownFrom() throws Exception {
        assertPaymentStatus(UUID.randomUUID(), accountBId,
                BigDecimal.TEN,
                UUID.randomUUID().toString(), 404);
    }

    @Test @DisplayName("Unknown toAccount → 404")
    void unknownTo() throws Exception {
        assertPaymentStatus(accountAId, UUID.randomUUID(),
                BigDecimal.TEN,
                UUID.randomUUID().toString(), 404);
    }
}