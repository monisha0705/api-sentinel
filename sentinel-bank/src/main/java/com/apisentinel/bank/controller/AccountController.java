package com.apisentinel.bank.controller;

import com.apisentinel.bank.dto.AccountResponse;
import com.apisentinel.bank.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            Authentication authentication) {

        return ResponseEntity.ok(
                accountService.getMyAccounts(authentication)
        );
    }

    @GetMapping("/{id}")
public ResponseEntity<AccountResponse> getAccountById(
        @PathVariable Long id,
        Authentication authentication) {

    return ResponseEntity.ok(
            accountService.getAccountById(id, authentication)
    );
}
}