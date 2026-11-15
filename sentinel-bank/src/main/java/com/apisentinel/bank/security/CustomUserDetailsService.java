package com.apisentinel.bank.security;

import com.apisentinel.bank.entity.BankUser;
import com.apisentinel.bank.repository.BankUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final BankUserRepository bankUserRepository;

    public CustomUserDetailsService(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        BankUser bankUser = bankUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username
                ));

        return User.withUsername(bankUser.getUsername())
                .password(bankUser.getPasswordHash())
                .roles(bankUser.getRole())
                .disabled(!bankUser.isActive())
                .build();
    }
}