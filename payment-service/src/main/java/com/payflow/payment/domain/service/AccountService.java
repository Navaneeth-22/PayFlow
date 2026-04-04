package com.payflow.payment.domain.service;

import com.payflow.payment.api.dto.AccountResponse;
import com.payflow.payment.api.dto.CreateAccountRequest;
import com.payflow.payment.domain.model.Account;
import com.payflow.payment.domain.model.AccountStatus;
import com.payflow.payment.domain.repository.AccountRepository;
import com.payflow.payment.exception.AccountNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for userId: {}", request.getUserId());

        Account account = Account.builder()
                .userId(request.getUserId())
                .currency(request.getCurrency())
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created with id: {}", saved.getId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        return toResponse(account);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }
}