package com.payflow.payment;

import com.payflow.payment.api.dto.CreatePaymentRequest;
import com.payflow.payment.domain.model.Account;
import com.payflow.payment.domain.model.AccountStatus;
import com.payflow.payment.domain.repository.AccountRepository;
import com.payflow.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Idempotency Concurrency Tests")
class IdempotencyIT extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("100 concurrent requests same key → exactly 1 payment in DB")
    void concurrentDuplicates_exactlyOnePayment() throws Exception {

        Account accountA = accountRepository.save(Account.builder()
                .userId(UUID.randomUUID()).currency("INR")
                .status(AccountStatus.ACTIVE).build());

        Account accountB = accountRepository.save(Account.builder()
                .userId(UUID.randomUUID()).currency("INR")
                .status(AccountStatus.ACTIVE).build());

        String idempotencyKey = "concurrent-" + UUID.randomUUID();
        int threadCount = 100;

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setFromAccountId(accountA.getId());
        request.setToAccountId(accountB.getId());
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("INR");

        String body = objectMapper.writeValueAsString(request);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        AtomicInteger success2xx  = new AtomicInteger(0);
        AtomicInteger errors      = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    var result = mockMvc.perform(
                            post("/api/v1/payments")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("X-Idempotency-Key", idempotencyKey)
                                    .content(body)
                    ).andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 202) {
                        success2xx.incrementAndGet();
                    } else {
                        errors.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(doneLatch.await(30, SECONDS)).isTrue();
        executor.shutdown();

        System.out.printf(
                "Idempotency test: %d succeeded, %d errors (MockMvc concurrency limitation)%n",
                success2xx.get(), errors.get()
        );
        await().atMost(10, SECONDS).untilAsserted(() -> {
            long count = paymentRepository.findAll().stream()
                    .filter(p -> idempotencyKey.equals(p.getIdempotencyKey()))
                    .count();
            assertThat(count)
                    .as("Idempotency broken — found %d payments for 1 key. " +
                            "UNIQUE constraint or Redis fast path failed.", count)
                    .isEqualTo(1);
        });
        assertThat(success2xx.get())
                .as("At least 1 request must succeed")
                .isGreaterThanOrEqualTo(1);
    }
}