package org.example.repo;

import org.example.model.PaymentRecord;
import org.example.model.PaymentStatus;
import org.example.model.Currency;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PaymentLog implements PaymentRepository {

    private final List<PaymentRecord> records = new ArrayList<>();

    @Override
    public PaymentRecord save(PaymentRecord paymentRecord) {
        records.add(paymentRecord);
        return paymentRecord;
    }

    @Override
    public Optional<PaymentRecord> findByReference(UUID reference) {
        for (PaymentRecord record : records) {
            if (record.getPayment().getReference().equals(reference)) {
                return Optional.of(record);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<PaymentRecord> findAll() {
        return List.copyOf(records);
    }

    @Override
    public List<PaymentRecord> findByStatus(PaymentStatus status) {
        List<PaymentRecord> matchingRecords = new ArrayList<>();

        for (PaymentRecord record : records) {
            if (record.getPayment().getStatus() == status) {
                matchingRecords.add(record);
            }
        }

        return matchingRecords;
    }

    @Override
    public List<PaymentRecord> findByCurrency(Currency currency) {
        List<PaymentRecord> matchingRecords = new ArrayList<>();

        for (PaymentRecord record : records) {
            if (record.getPayment().getCurrency() == currency) {
                matchingRecords.add(record);
            }
        }

        return matchingRecords;
    }

    @Override
    public PaymentRecord update(PaymentRecord paymentRecord) {
        UUID reference = paymentRecord.getPayment().getReference();

        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).getPayment().getReference().equals(reference)) {
                records.set(index, paymentRecord);
                return paymentRecord;
            }
        }

        throw new IllegalArgumentException("Payment not found: " + reference);
    }

    @Override
    public void deleteByReference(UUID reference) {
        Iterator<PaymentRecord> iterator = records.iterator();

        while (iterator.hasNext()) {
            PaymentRecord record = iterator.next();

            if (record.getPayment().getReference().equals(reference)) {
                iterator.remove();
                return;
            }
        }
    }
}
