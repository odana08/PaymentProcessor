package org.example.repo;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
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
    void givenPayment_whenSaved_thenReturnsAndStoresPayment() {
        Payment payment = payment(Currency.JOD);

        Payment saved = repository.save(payment);

        assertSame(payment, saved);
        assertEquals(1, repository.findAll().size());
        assertSame(payment, repository.findAll().getFirst());
    }

    @Test
    void givenSavedRecord_whenFoundByReference_thenReturnsRecord() {
        Payment payment = payment(Currency.USD);
        repository.save(payment);

        Payment result = repository.findByReference(payment.getReference()).orElseThrow();

        assertSame(payment, result);
    }

    @Test
    void givenUnknownReference_whenFound_thenReturnsEmpty() {
        assertTrue(repository.findByReference(UUID.randomUUID()).isEmpty());
    }

    @Test
    void givenRecordsWithDifferentStatuses_whenFoundByStatus_thenReturnsMatches() {
        Payment created = payment(Currency.JOD);
        Payment processed = payment(Currency.JOD);
        processed.markAsProcessed();
        repository.save(created);
        repository.save(processed);

        assertEquals(List.of(created), repository.findByStatus(PaymentStatus.CREATED));
        assertEquals(List.of(processed), repository.findByStatus(PaymentStatus.PROCESSED));
    }

    @Test
    void givenRecordsWithDifferentCurrencies_whenFoundByCurrency_thenReturnsMatches() {
        Payment jodPayment = payment(Currency.JOD);
        Payment usdPayment = payment(Currency.USD);
        repository.save(jodPayment);
        repository.save(usdPayment);

        assertEquals(List.of(jodPayment), repository.findByCurrency(Currency.JOD));
        assertEquals(List.of(usdPayment), repository.findByCurrency(Currency.USD));
    }

    @Test
    void givenExistingRecord_whenUpdated_thenReplacesStoredRecord() {
        Payment payment = payment(Currency.JOD);
        payment.setFeeInCents(new BigDecimal("100"));
        repository.save(payment);
        payment.setFeeInCents(new BigDecimal("250"));

        Payment updated = repository.update(payment);

        assertSame(payment, updated);
        assertSame(payment, repository.findByReference(payment.getReference()).orElseThrow());
        assertEquals(new BigDecimal("250"), updated.getFeeInCents());
    }

    @Test
    void givenUnknownRecord_whenUpdated_thenThrowsException() {
        Payment payment = payment(Currency.JOD);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(payment)
        );

        assertEquals("Payment not found: " + payment.getReference(), exception.getMessage());
    }

    @Test
    void givenSavedRecord_whenDeletedByReference_thenRemovesRecord() {
        Payment payment = payment(Currency.JOD);
        repository.save(payment);

        repository.deleteByReference(payment.getReference());

        assertFalse(repository.findByReference(payment.getReference()).isPresent());
        assertTrue(repository.findAll().isEmpty());
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
