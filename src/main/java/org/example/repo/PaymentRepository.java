package org.example.repo;

import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.example.model.Currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByReference(UUID reference);

    List<Payment> findAll();

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByCurrency(Currency currency);

    Payment update(Payment payment);

    void deleteByReference(UUID reference);
}
