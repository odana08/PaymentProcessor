package org.example.repo;

import jakarta.persistence.EntityManager;
import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.example.model.PaymentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void givenPaymentWithNotificationChannels_whenSavedAndFound_thenRestoresTheAggregate() {
        // Given
        Payment payment = payment("DOMESTIC_FEE", Currency.JOD, "10000", "EMAIL", "SMS");
        payment.setFeeInCents(new BigDecimal("150"));
        payment.markAsProcessed();

        // When
        repository.saveAndFlush(payment);
        entityManager.clear();
        Payment found = repository.findByReference(payment.getReference()).orElseThrow();

        // Then
        assertEquals(payment.getReference(), found.getReference());
        assertEquals(new BigDecimal("150.00"), found.getFeeInCents());
        assertEquals(List.of("EMAIL", "SMS"), found.getNotificationChannels().stream()
                .map(NotificationChannel::getName)
                .toList());
    }

    @Test
    void givenPaymentsWithDifferentStatusAndCurrency_whenDerivedQueriesRun_thenReturnsMatchingPayments() {
        // Given
        Payment jod = payment("DOMESTIC_FEE", Currency.JOD, "10000", "EMAIL");
        Payment usd = payment("CHEQUE_FEE", Currency.USD, "25000", "SMS");
        usd.markAsProcessed();
        repository.saveAllAndFlush(List.of(jod, usd));
        entityManager.clear();

        // When
        List<Payment> createdPayments = repository.findByStatus(PaymentStatus.CREATED);
        List<Payment> usdPayments = repository.findByCurrency(Currency.USD);

        // Then
        assertEquals(List.of(jod.getReference()), createdPayments.stream()
                .map(Payment::getReference)
                .toList());
        assertEquals(List.of(usd.getReference()), usdPayments.stream()
                .map(Payment::getReference)
                .toList());
    }

    @Test
    void givenSeveralPayments_whenSearchingWithFiltersAndPagination_thenReturnsMatchingPage() {
        // Given
        Payment first = payment("DOMESTIC_FEE", Currency.JOD, "10000", "EMAIL");
        Payment second = payment("INTERNATIONAL_FEE", Currency.JOD, "20000", "SMS");
        Payment third = payment("DOMESTIC_FEE", Currency.USD, "30000", "EMAIL");
        first.markAsProcessed();
        second.markAsProcessed();
        third.markAsProcessed();
        repository.saveAllAndFlush(List.of(first, second, third));
        entityManager.clear();

        // When
        Page<Payment> page = repository.search(
                PaymentStatus.PROCESSED,
                Currency.JOD,
                "domestic_fee",
                PageRequest.of(0, 1, Sort.by("reference"))
        );

        // Then
        assertEquals(1, page.getTotalElements());
        assertEquals(first.getReference(), page.getContent().getFirst().getReference());
        assertTrue(page.isFirst());
    }

    @Test
    void givenSavedPayment_whenDeletingByReference_thenRemovesThePaymentAggregate() {
        // Given
        Payment payment = payment("DOMESTIC_FEE", Currency.JOD, "10000", "EMAIL", "SMS");
        repository.saveAndFlush(payment);

        // When
        long deleted = repository.deleteByReference(payment.getReference());
        repository.flush();

        // Then
        assertEquals(1, deleted);
        assertFalse(repository.existsByReference(payment.getReference()));
    }

    private Payment payment(String type, Currency currency, String amount, String... channels) {
        List<NotificationChannel> notificationChannels = java.util.Arrays.stream(channels)
                .map(NotificationChannel::new)
                .toList();
        return new Payment(
                new PaymentType(type),
                new BigDecimal(amount),
                currency,
                notificationChannels
        );
    }
}
