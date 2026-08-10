package org.example.repo;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentRecord;
import org.example.model.PaymentStatus;
import org.example.model.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentLogTest {

    private final PaymentRepository repository = new PaymentLog();

    @Test
    void givenNewRepository_whenAllRecordsRequested_thenReturnsEmptyList() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void givenPaymentRecord_whenSaved_thenReturnsAndStoresRecord() {
        PaymentRecord record = paymentRecord(Currency.JOD);

        PaymentRecord saved = repository.save(record);

        assertSame(record, saved);
        assertEquals(1, repository.findAll().size());
        assertSame(record, repository.findAll().getFirst());
    }

    @Test
    void givenSavedRecord_whenFoundByReference_thenReturnsRecord() {
        PaymentRecord record = paymentRecord(Currency.USD);
        repository.save(record);

        PaymentRecord result = repository.findByReference(record.getPayment().getReference()).orElseThrow();

        assertSame(record, result);
    }

    @Test
    void givenUnknownReference_whenFound_thenReturnsEmpty() {
        assertTrue(repository.findByReference(UUID.randomUUID()).isEmpty());
    }

    @Test
    void givenRecordsWithDifferentStatuses_whenFoundByStatus_thenReturnsMatches() {
        PaymentRecord created = paymentRecord(Currency.JOD);
        PaymentRecord processed = paymentRecord(Currency.JOD);
        processed.getPayment().markAsProcessed();
        repository.save(created);
        repository.save(processed);

        assertEquals(List.of(created), repository.findByStatus(PaymentStatus.CREATED));
        assertEquals(List.of(processed), repository.findByStatus(PaymentStatus.PROCESSED));
    }

    @Test
    void givenRecordsWithDifferentCurrencies_whenFoundByCurrency_thenReturnsMatches() {
        PaymentRecord jodRecord = paymentRecord(Currency.JOD);
        PaymentRecord usdRecord = paymentRecord(Currency.USD);
        repository.save(jodRecord);
        repository.save(usdRecord);

        assertEquals(List.of(jodRecord), repository.findByCurrency(Currency.JOD));
        assertEquals(List.of(usdRecord), repository.findByCurrency(Currency.USD));
    }

    @Test
    void givenExistingRecord_whenUpdated_thenReplacesStoredRecord() {
        Payment payment = payment(Currency.JOD);
        PaymentRecord original = new PaymentRecord(payment, new BigDecimal("100"));
        PaymentRecord replacement = new PaymentRecord(payment, new BigDecimal("250"));
        repository.save(original);

        PaymentRecord updated = repository.update(replacement);

        assertSame(replacement, updated);
        assertSame(replacement, repository.findByReference(payment.getReference()).orElseThrow());
    }

    @Test
    void givenUnknownRecord_whenUpdated_thenThrowsException() {
        PaymentRecord record = paymentRecord(Currency.JOD);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(record)
        );

        assertEquals("Payment not found: " + record.getPayment().getReference(), exception.getMessage());
    }

    @Test
    void givenSavedRecord_whenDeletedByReference_thenRemovesRecord() {
        PaymentRecord record = paymentRecord(Currency.JOD);
        repository.save(record);

        repository.deleteByReference(record.getPayment().getReference());

        assertFalse(repository.findByReference(record.getPayment().getReference()).isPresent());
        assertTrue(repository.findAll().isEmpty());
    }

    private PaymentRecord paymentRecord(Currency currency) {
        return new PaymentRecord(payment(currency), new BigDecimal("150"));
    }

    private Payment payment(Currency currency) {
        return new Payment(
                new PaymentType("DOMESTIC_FEE"),
                new BigDecimal("10000"),
                currency,
                new NotificationChannel("EMAIL")
        );
    }
}
