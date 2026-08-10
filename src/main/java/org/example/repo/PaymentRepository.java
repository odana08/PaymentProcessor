package org.example.repo;

import org.example.model.PaymentRecord;
import org.example.model.PaymentStatus;
import org.example.model.Currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    PaymentRecord save(PaymentRecord paymentRecord);

    Optional<PaymentRecord> findByReference(UUID reference);

    List<PaymentRecord> findAll();

    List<PaymentRecord> findByStatus(PaymentStatus status);

    List<PaymentRecord> findByCurrency(Currency currency);

    PaymentRecord update(PaymentRecord paymentRecord);

    void deleteByReference(UUID reference);
}
