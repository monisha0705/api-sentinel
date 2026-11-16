
package com.apisentinel.bank.repository;

import com.apisentinel.bank.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventRepository
        extends JpaRepository<PaymentEvent, Long> {

    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAsc(
            java.util.UUID paymentId
    );
}
