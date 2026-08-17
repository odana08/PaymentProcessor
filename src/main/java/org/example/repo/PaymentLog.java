package org.example.repo;

import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.example.model.Currency;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PaymentLog implements PaymentRepository {

    private final List<Payment> payments = new ArrayList<>();

    @Override
    public Payment save(Payment payment) {
        payments.add(payment);
        return payment;
    }

    @Override
    public Optional<Payment> findByReference(UUID reference) {
        for (Payment payment : payments) {
            if (payment.getReference().equals(reference)) {
                return Optional.of(payment);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<Payment> findAll() {
        return List.copyOf(payments);
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        List<Payment> matchingPayments = new ArrayList<>();

        for (Payment payment : payments) {
            if (payment.getStatus() == status) {
                matchingPayments.add(payment);
            }
        }

        return matchingPayments;
    }

    @Override
    public List<Payment> findByCurrency(Currency currency) {
        List<Payment> matchingPayments = new ArrayList<>();

        for (Payment payment : payments) {
            if (payment.getCurrency() == currency) {
                matchingPayments.add(payment);
            }
        }

        return matchingPayments;
    }

    @Override
    public Payment update(Payment payment) {
        UUID reference = payment.getReference();

        for (int index = 0; index < payments.size(); index++) {
            if (payments.get(index).getReference().equals(reference)) {
                payments.set(index, payment);
                return payment;
            }
        }

        throw new IllegalArgumentException("Payment not found: " + reference);
    }

    @Override
    public void deleteByReference(UUID reference) {
        Iterator<Payment> iterator = payments.iterator();

        while (iterator.hasNext()) {
            Payment payment = iterator.next();

            if (payment.getReference().equals(reference)) {
                iterator.remove();
                return;
            }
        }
    }
}
