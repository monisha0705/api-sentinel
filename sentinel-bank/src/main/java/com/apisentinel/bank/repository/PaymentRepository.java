package com.apisentinel.bank.repository;

import com.apisentinel.bank.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCreatedById(Long userId);
}
