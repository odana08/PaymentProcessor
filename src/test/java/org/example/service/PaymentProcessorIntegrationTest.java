package org.example.service;

import jakarta.persistence.EntityManagerFactory;
import org.example.dto.CreatePaymentRequest;
import org.example.dto.PageResponse;
import org.example.dto.PatchPaymentRequest;
import org.example.dto.PaymentResponse;
import org.example.dto.UpdatePaymentRequest;
import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.repo.PaymentRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(PaymentProcessorIntegrationTest.FailingNotificationConfiguration.class)
class PaymentProcessorIntegrationTest {

    @Autowired
    private PaymentProcessor processor;

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clearPayments() {
        repository.deleteAll();
    }

    @Test
    void patchUsesDirtyCheckingToPersistRecalculatedFeeWithoutExplicitSave() {
        PaymentResponse created = processor.createPayment(new CreatePaymentRequest(
                "INTERNATIONAL_FEE",
                new BigDecimal("1000"),
                Currency.JOD,
                List.of("EMAIL")
        ));

        PaymentResponse updated = processor.patchPayment(
                created.reference(),
                new PatchPaymentRequest(null, new BigDecimal("2000"), null, null)
        );

        PaymentResponse reloaded = processor.getPayment(created.reference());
        assertEquals(new BigDecimal("40.00"), updated.feeInCents());
        assertEquals(new BigDecimal("2000.00"), reloaded.amountInCents());
        assertEquals(new BigDecimal("40.00"), reloaded.feeInCents());
        assertEquals(1, repository.count());
    }

    @Test
    void notificationFailureRollsBackTheFlushedInsertAtomically() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "DOMESTIC_FEE",
                new BigDecimal("1000"),
                Currency.JOD,
                List.of("FAIL")
        );

        assertThrows(
                IllegalStateException.class,
                () -> processor.createPayment(request)
        );

        assertEquals(0, repository.count());
    }

    @Test
    void subCentInternationalFeeUsesTheSameRoundingInTheResponseAndDatabase() {
        PaymentResponse created = processor.createPayment(new CreatePaymentRequest(
                "INTERNATIONAL_FEE",
                new BigDecimal("50.25"),
                Currency.JOD,
                List.of("EMAIL")
        ));

        PaymentResponse reloaded = processor.getPayment(created.reference());

        assertEquals(new BigDecimal("1.01"), created.feeInCents());
        assertEquals(new BigDecimal("1.01"), reloaded.feeInCents());
    }

    @Test
    void duplicateNotificationChannelsAreRejectedBeforeAnythingIsStored() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "DOMESTIC_FEE",
                new BigDecimal("1000"),
                Currency.JOD,
                List.of("EMAIL", "EMAIL")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> processor.createPayment(request)
        );

        assertEquals(0, repository.count());
    }

    @Test
    void replaceUpdatesEveryEditableFieldAndDeleteRemovesTheAggregate() {
        PaymentResponse created = processor.createPayment(request("DOMESTIC_FEE", "1000", "EMAIL"));

        PaymentResponse replaced = processor.replacePayment(
                created.reference(),
                new UpdatePaymentRequest(
                        "CHEQUE_FEE",
                        new BigDecimal("25000"),
                        Currency.USD,
                        List.of("SMS")
                )
        );

        assertEquals("CHEQUE_FEE", replaced.paymentType());
        assertEquals(Currency.USD, replaced.currency());
        assertEquals(List.of("SMS"), replaced.notificationChannels());
        assertEquals(0, replaced.feeInCents().compareTo(new BigDecimal("500")));

        PaymentResponse reloaded = processor.getPayment(created.reference());
        assertEquals("CHEQUE_FEE", reloaded.paymentType());
        assertEquals(Currency.USD, reloaded.currency());
        assertEquals(List.of("SMS"), reloaded.notificationChannels());
        assertEquals(0, reloaded.amountInCents().compareTo(new BigDecimal("25000")));
        assertEquals(0, reloaded.feeInCents().compareTo(new BigDecimal("500")));

        processor.deletePayment(created.reference());

        assertThrows(NoSuchElementException.class, () -> processor.getPayment(created.reference()));
        assertEquals(0, repository.count());
    }

    @Test
    void pagedResponseBatchFetchesChannelsWithoutOneQueryPerPayment() {
        processor.createPayment(request("DOMESTIC_FEE", "1000", "EMAIL"));
        processor.createPayment(request("CHEQUE_FEE", "2000", "SMS"));
        processor.createPayment(request("INTERNATIONAL_FEE", "3000", "EMAIL"));

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResponse<PaymentResponse> page = processor.findPayments(
                null,
                null,
                null,
                PageRequest.of(0, 2, Sort.by("reference"))
        );

        assertEquals(2, page.content().size());
        assertEquals(3, page.totalElements());
        assertTrue(
                statistics.getPrepareStatementCount() <= 3,
                "The page, count, and batched channel load should not become one channel query per payment"
        );
    }

    private CreatePaymentRequest request(String type, String amount, String channel) {
        return new CreatePaymentRequest(
                type,
                new BigDecimal(amount),
                Currency.JOD,
                List.of(channel)
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingNotificationConfiguration {

        @Bean
        NotificationSender failingNotificationSender() {
            return new NotificationSender() {
                @Override
                public NotificationChannel supportedChannel() {
                    return new NotificationChannel("FAIL");
                }

                @Override
                public void send(Payment payment) {
                    throw new IllegalStateException("Notification failed");
                }
            };
        }
    }
}
