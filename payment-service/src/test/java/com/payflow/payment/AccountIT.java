package com.payflow.payment;

import com.payflow.payment.api.dto.CreateAccountRequest;
import com.payflow.payment.domain.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Account Integration Tests")
class AccountIT extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Create account → 201 → stored in DB")
    void createAccount_returns201_storedInDb() throws Exception {

        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(UUID.randomUUID());
        request.setCurrency("INR");

        String response = mockMvc.perform(
                        post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );

        assertThat(accountRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("Get account by ID → 200")
    void getAccount_returns200() throws Exception {

        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(UUID.randomUUID());
        request.setCurrency("INR");

        String created = mockMvc.perform(
                        post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(
                objectMapper.readTree(created).get("id").asText()
        );

        mockMvc.perform(get("/api/v1/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("Get non-existent account → 404")
    void getNonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}