
package com.apisentinel.bank.service;

import com.apisentinel.bank.dto.AccountResponse;
import com.apisentinel.bank.entity.BankUser;
import com.apisentinel.bank.repository.AccountRepository;
import com.apisentinel.bank.repository.BankUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BankUserRepository bankUserRepository;

    public AccountService(
            AccountRepository accountRepository,
            BankUserRepository bankUserRepository) {
        this.accountRepository = accountRepository;
        this.bankUserRepository = bankUserRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(Authentication authentication) {
        String username = authentication.getName();

        BankUser user = bankUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return accountRepository.findByOwnerId(user.getId())
                .stream()
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getBalance(),
                        account.getCurrency(),
                        account.getStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(
            Long accountId,
            Authentication authentication) {

        String username = authentication.getName();

        BankUser user = bankUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return accountRepository.findById(accountId)
                .filter(account -> account.getOwner().getId().equals(user.getId()))
                .map(account -> new AccountResponse(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getBalance(),
                        account.getCurrency(),
                        account.getStatus()
                ))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found"
                ));
    }
}
