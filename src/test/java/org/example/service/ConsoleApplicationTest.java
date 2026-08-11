package org.example.service;

import org.example.repo.PaymentLog;
import org.example.repo.PaymentRepository;
import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentRecord;
import org.example.model.PaymentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleApplicationTest {

    @Test
    void givenValidInput_whenPaymentProcessed_thenStoresAndDisplaysPayment() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(
                List.of(new DomesticFeeRule()),
                List.of(new EmailNotificationSender()),
                repository
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1\n1\n1000\n1\n1\n2\n0\n"),
                new PrintStream(output),
                processor,
                repository
        );

        application.run();

        assertEquals(1, repository.findAll().size());
        assertTrue(output.toString().contains("Payment processed successfully."));
        assertTrue(output.toString().contains("Currency: JOD"));
        assertTrue(output.toString().contains("Status: PROCESSED"));
    }

    @Test
    void givenInvalidAmount_whenPaymentEntered_thenRetriesBeforeContinuing() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(
                List.of(new DomesticFeeRule()),
                List.of(new EmailNotificationSender()),
                repository
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1\n1\n0\n1000\n1\n1\n0\n"),
                new PrintStream(output),
                processor,
                repository
        );

        application.run();

        assertEquals(1, repository.findAll().size());
        assertTrue(output.toString().contains("Invalid amount. Enter a number greater than zero."));
        assertTrue(output.toString().contains("Payment processed successfully."));
    }

    @Test
    void givenUnavailableCurrency_whenPaymentEntered_thenShowsFriendlyMessageAndRetries() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(
                List.of(new DomesticFeeRule()),
                List.of(new EmailNotificationSender()),
                repository
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1\n1\n1000\n99\n1\n1\n0\n"),
                new PrintStream(output), processor, repository
        );

        application.run();

        assertEquals(1, repository.findAll().size());
        assertTrue(output.toString().contains("Currency not available."));
        assertTrue(output.toString().contains("1. Transfer payment"));
    }

    @Test
    void givenRegisteredRules_whenMenuOpened_thenDisplaysPaymentTypesDynamically() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(
                List.of(new DomesticFeeRule(), new ChequeFeeRule()), List.of(), repository
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1\n"), new PrintStream(output), processor, repository
        );

        application.run();

        assertTrue(output.toString().contains("1. DOMESTIC"));
        assertTrue(output.toString().contains("2. CHEQUE"));
        assertFalse(output.toString().contains("DOMESTIC_FEE"));
    }

    @Test
    void givenCopiedConsoleLabels_whenPastedAsInput_thenProcessesPayment() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(
                List.of(new DomesticFeeRule()),
                List.of(new EmailNotificationSender(), new SMSNotificationSender()),
                repository
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1. Transfer payment\n1. DOMESTIC\n1000\n1. JOD\n1. EMAIL, 2. SMS\n0. Exit\n"),
                new PrintStream(output), processor, repository
        );

        application.run();

        assertEquals(1, repository.findAll().size());
        assertTrue(output.toString().contains("Payment processed successfully."));
        assertFalse(output.toString().contains("Exception"));
    }

    @Test
    void givenInvalidReference_whenDeletingPayment_thenRetriesUntilReferenceIsValid() {
        PaymentRepository repository = new PaymentLog();
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"), new BigDecimal("1000"), Currency.JOD,
                new NotificationChannel("EMAIL")
        );
        PaymentRecord record = new PaymentRecord(payment, new BigDecimal("150"));
        repository.save(record);
        PaymentProcessor processor = new PaymentProcessor(List.of(), List.of(), repository);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("4\nnot-a-reference\n" + payment.getReference() + "\n2\n0\n"),
                new PrintStream(output), processor, repository
        );

        application.run();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(output.toString().contains("Invalid payment reference. Please try again."));
        assertTrue(output.toString().contains("Payment deleted."));
        assertTrue(output.toString().contains("No payments found."));
    }
}
