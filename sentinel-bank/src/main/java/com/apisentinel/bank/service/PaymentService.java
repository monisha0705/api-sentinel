
package com.apisentinel.bank.service;

import com.apisentinel.bank.dto.PaymentRequest;
import com.apisentinel.bank.dto.PaymentResponse;
import com.apisentinel.bank.entity.Account;
import com.apisentinel.bank.entity.BankUser;
import com.apisentinel.bank.entity.Payment;
import com.apisentinel.bank.entity.PaymentEvent;
import com.apisentinel.bank.repository.AccountRepository;
import com.apisentinel.bank.repository.BankUserRepository;
import com.apisentinel.bank.repository.PaymentEventRepository;
import com.apisentinel.bank.repository.PaymentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final BankUserRepository bankUserRepository;
    private final PaymentEventRepository paymentEventRepository;

    @Value("${sentinel.bank.authorization-mode:SECURE}")
    private String authorizationMode;

    public PaymentService(
            PaymentRepository paymentRepository,
            AccountRepository accountRepository,
            BankUserRepository bankUserRepository,
            PaymentEventRepository paymentEventRepository) {

        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.bankUserRepository = bankUserRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request,
            Authentication authentication) {

        BankUser user = getAuthenticatedUser(authentication);

        Account account = accountRepository.findById(request.sourceAccountId())
                .filter(a -> a.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Source account is not active");
        }

        if (!account.getCurrency().equals(request.currency())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Currency does not match source account");
        }

        Payment payment = new Payment(
                account,
                user,
                request.amount(),
                request.currency(),
                request.beneficiaryReference()
        );

        Payment savedPayment = paymentRepository.save(payment);

        recordEvent(
                savedPayment,
                user,
                "CREATED",
                null,
                savedPayment.getStatus(),
                Map.of("beneficiaryReference",
                        savedPayment.getBeneficiaryReference())
        );

        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(
            UUID paymentId,
            Authentication authentication) {

        BankUser user = getAuthenticatedUser(authentication);

        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> p.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse approvePayment(
            UUID paymentId,
            Authentication authentication) {

        BankUser user = getAuthenticatedUser(authentication);

        // SECURE mode enforces administrator-only approval.
        // VULNERABLE mode intentionally disables this check
        // for controlled security testing.
        if (!"VULNERABLE".equalsIgnoreCase(authorizationMode)
                && !"ADMIN".equals(user.getRole())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can approve payments");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        String previousStatus = payment.getStatus();

        try {
            payment.approve(user);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, exception.getMessage());
        }

        recordEvent(
                payment,
                user,
                "APPROVED",
                previousStatus,
                payment.getStatus(),
                null
        );

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(
            UUID paymentId,
            Authentication authentication) {

        BankUser user = getAuthenticatedUser(authentication);

        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> p.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        String previousStatus = payment.getStatus();

        try {
            payment.cancel();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, exception.getMessage());
        }

        recordEvent(
                payment,
                user,
                "CANCELLED",
                previousStatus,
                payment.getStatus(),
                null
        );

        return toResponse(payment);
    }

    private void recordEvent(
            Payment payment,
            BankUser actor,
            String eventType,
            String fromStatus,
            String toStatus,
            Map<String, Object> details) {

        PaymentEvent event = new PaymentEvent(
                payment,
                actor,
                eventType,
                fromStatus,
                toStatus,
                details
        );

        paymentEventRepository.save(event);
    }

    private BankUser getAuthenticatedUser(Authentication authentication) {
        return bankUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getSourceAccount().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getBeneficiaryReference(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
